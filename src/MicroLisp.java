import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;
import java.math.BigInteger;

public class MicroLisp {
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String BLUE = "\u001B[38;2;45;199;193m";
    public static final String YELLOW = "\u001b[0;93m";
    public static final String ORANGE = "\u001b[38;2;255;140;0m";

    public static void main(String[] args){
        // ----- Create Initial Environment --------- 
        Environment environment = makeGlobalEnv();
        // ----- Load and display the banner and text ---------
        if (args[0].equals("-i")){
            InputStream in = MicroLisp.class.getResourceAsStream("/banner.txt");
            if (in != null) {
                try {
                    String banner = new String(in.readAllBytes()).replaceAll("\\s+$", "");
                    System.out.print(banner); 
                    in.close();
                } catch (IOException e) {
                    System.out.println(RED+"Error reading banner: "+RESET + e.getMessage());
                }
            } else {
                System.out.println(RED+ "Could not find banner"+RESET);
            }
            System.out.println("\n"+YELLOW + "              MicroLisp v1.0 - ©Jordan Jacobson 2025" + RESET);
            System.out.println("Type "+BLUE+":exit"+RESET+" to quit, "+BLUE+":load filename"+RESET+" to load a file");
            
            // ------ Load files on opening if passed file names ----- 
            Token eof = new Token<>("EOF","EOF");
            String src; 
            if (args.length > 1){
                for (int i=1;i<args.length;i++){
                    try {
                        src = Files.readString(Path.of(args[i]));
                        Parser parser = new Parser(src);
                        Node current = parser.parse();
                        Object result = null;
                        while(!((Token) current.value).type().equals("EOF")){
                            Evaluator.eval(current, environment);
                            current = parser.parse();
                        }
                        if (result != null) {
                            String out = result.toString();
                            if (out.isBlank()) {
                                System.out.print("\u001b[1A\u001b[2K"); // clear extra line
                            } else {
                                System.out.println(out);
                            }
                        }
                        System.out.println(args[i]+ GREEN + " loaded successfully" + RESET); 
                    }
                    catch (IOException e){
                        System.out.println(RED +"Could not load file "+ RESET + args[i]);
                        System.out.println(e);
                    }
                }
                repl(environment);
            }
            else {
                repl(environment);
            }
        } else {
            // ------ Load files on opening if passed file names ----- 
            Token eof = new Token<>("EOF","EOF");
            String src; 
            if (args.length > 0){
                for (int i=0;i<args.length;i++){
                    try {
                        src = Files.readString(Path.of(args[i]));
                        Parser parser = new Parser(src);
                        Node current = parser.parse();
                        Object result = null;
                        while(!((Token) current.value).type().equals("EOF")){
                            result = Evaluator.eval(current, environment);
                            current = parser.parse();
                        }                        
                        
                        if (result != null) {
                            String out = result.toString();
                            if (out.isBlank()) {
                                // Clear current (blank) line but stay put
                                System.out.print("\u001b[2K\r");
                            } else {
                                System.out.println(out);
                            }
                        }

                    }
                    catch (IOException e){
                        System.out.println(RED +"Could not load file "+ RESET + args[i]);
                        System.out.println(e);
                    }
                } 
            }

        }
    }

    static void repl(Environment environment) {
        Scanner scanner = new Scanner(System.in); 
        while(true){
            System.out.print(ORANGE + ">>>"+ RESET);
            String input = scanner.nextLine();
            if(input.equals(":exit")){
                break;
            }
            else if(input.startsWith(":load")){
                try {
                String file = input.substring(5).trim();
                String src = Files.readString(Path.of(file));
                Parser l = new Parser(src);
                Node current = l.parse();
                while(!((Token) current.value).type().equals("EOF")){
                    Evaluator.eval(current, environment);
                    current = l.parse();
                }
                System.out.println(file + GREEN +" loaded successfully" + RESET);
                }
                catch (IOException e){
                System.out.println(RED + "Could not load file "+ RESET + input.substring(5).trim());
                System.out.println(e);
                }
                
            } else if(input.equals("")){
                System.out.print("");
            }
            else {
                Parser p = new Parser(input);
                Object result = Evaluator.eval(p.parse(),environment);
                System.out.println(result.toString());
            }
        }
    }

    public static Environment makeGlobalEnv() {
            Environment environment = new Environment(
                new Pair<>("else", "#t"),
                //new Pair<>("null?",(Function<Object,String>) (x) -> "()".equals(x.toString()) ? "#t" : "#f"),                
                new Pair<>("even?", (Function<BigInteger, String>) (x) ->
                    x.mod(BigInteger.valueOf(2)).equals(BigInteger.ZERO) ? "#t" : "#f"
                ),
                new Pair<>("odd?", (Function<BigInteger, String>) (x) ->
                    x.mod(BigInteger.valueOf(2)).equals(BigInteger.ZERO) ? "#f" : "#t"
                ),
                new Pair<>("!",(Function<String, String>) (x) -> x.equals("#t") ? "#f" : "#t"),
                new Pair<>("and", (BiFunction<String, String, String>) (p, q) ->
                    (p.equals("#t") && q.equals("#t")) ? "#t" : "#f"
                ),

                new Pair<>("or", (BiFunction<String, String, String>) (p, q) ->
                    (p.equals("#t") || q.equals("#t")) ? "#t" : "#f"
                ),
                new Pair<>("xor", (BiFunction<String, String, String>) (p, q) ->
                    ((p.equals("#t") && !q.equals("#t")) || (q.equals("#t") && !p.equals("#t"))) ? "#t" : "#f"
                ),
                new Pair<>("head", (Function<Object,Object>) (x) -> {
                    if (x instanceof LinkedList) {
                    return ((LinkedList<?>) x).head();
                    } else if (x instanceof String) {
                    String s = (String) x;
                    if (s.isEmpty()){return "";}
                    return "\""+String.valueOf(s.charAt(0))+"\"";
                    } else {
                    throw new RuntimeException("head: unsupported type " + x.getClass());
                    }
                }),
                new Pair<>("tail", (Function<Object,Object>) (x) -> {
                    if (x instanceof LinkedList) {
                    return ((LinkedList<?>) x).tail();
                    } else if (x instanceof String) {
                    String s = (String) x;
                    if (s.isEmpty()) {return "";} 
                    return s.substring(1);
                    } else {
                    throw new RuntimeException("tail: unsupported type " + x.getClass());
                    }
                }),                
                new Pair<>("length", (Function<LinkedList, BigInteger>) (xs) ->
                    BigInteger.valueOf(xs.size())
                ),
                new Pair<>("print", (Function<Object, Object>) x1 -> {
                    if (x1 == null) {
                        System.out.println("()");
                        return "";
                    }
                    String out = x1.toString();
                    // Drop wrapping quotes for string-like output
                    if (out.length() >= 2 && out.startsWith("\"") && out.endsWith("\"")) {
                        out = out.substring(1, out.length() - 1);
                    }
                    System.out.println(out);
                    return "";
                }),
                new Pair<>("printf", (Function<Object, Object>) x1 -> {
                    if (x1 == null) return "";
                    String out = x1.toString();
                    if (out.length() >= 2 && out.startsWith("\"") && out.endsWith("\"")) {
                        out = out.substring(1, out.length() - 1);
                    }
                    System.out.print(unescapeJava(out));
                    return "";
                }),
                new Pair<>("read", Evaluator.getPrimitive("READ")),
                new Pair<>("null?", (Function<Object,String>) (x) -> {
                    if (x == null) return "#t";
                    if (x instanceof LinkedList<?> l && l.isEmpty()) return "#t";
                    return "#f";
                })
            );
            environment.addFrame(
                new Pair<>("eval", (Function<Object,Object>) (str) -> {
                Parser p = new Parser(str.toString());
                return Evaluator.eval(p.parse(), environment);
                }),
                new Pair<>("cons", (BiFunction<Object,Object,LinkedList>) (fst, snd) -> {
                    if (fst == null) {
                        throw new SyntaxException("First element of a pair cannot be null");
                    }
                    if (snd == null) {
                        return new LinkedList<>(fst, (LinkedList<Object>) null);
                    }
                    if (snd instanceof LinkedList<?> tailList) {
                        @SuppressWarnings("unchecked")
                        LinkedList<Object> properTail = (LinkedList<Object>) tailList;
                        return new LinkedList<>(fst, properTail);
                    }
                    return new LinkedList<>(fst, snd);
                }) 
            );
            environment.addFrame(
                new Pair<>("+", (Function<LinkedList<?>, Object>) (args) -> {
                    BigInteger sum = BigInteger.ZERO;
                    LinkedList<?> current = args;
                    while (current != null && current.head() != null) {
                        sum = sum.add((BigInteger) current.head());
                        Object tail = current.tail();
                        if (!(tail instanceof LinkedList<?> next)) break;
                        current = next;
                    }
                    return sum;
                }),
                new Pair<>("-", (Function<LinkedList<?>, Object>) (args) -> {
                    if (args == null || args.head() == null)
                        return BigInteger.ZERO;
                    BigInteger result = (BigInteger) args.head();
                    Object tail = args.tail();
                    if (tail == null) return result.negate();
                    if (!(tail instanceof LinkedList<?> current)) return result;
                    while (current.head() != null) {
                        result = result.subtract((BigInteger) current.head());
                        Object nextTail = current.tail();
                        if (!(nextTail instanceof LinkedList<?> next)) break;
                        current = next;
                    }
                    return result;
                }),
                new Pair<>("*", (Function<LinkedList<?>, Object>) (args) -> {
                    BigInteger prod = BigInteger.ONE;
                    LinkedList<?> current = args;
                    while (current != null && current.head() != null) {
                        prod = prod.multiply((BigInteger) current.head());
                        Object tail = current.tail();
                        if (!(tail instanceof LinkedList<?> next)) break;
                        current = next;
                    }
                    return prod;
                }),
                new Pair<>("/", (Function<LinkedList<?>, Object>) (args) -> {
                    if (args == null || args.head() == null)
                        throw new SyntaxException("/ expects at least one argument");
                    BigInteger result = (BigInteger) args.head();
                    Object tail = args.tail();
                    if (!(tail instanceof LinkedList<?> current))
                        return result;
                    while (current.head() != null) {
                        result = result.divide((BigInteger) current.head());
                        Object nextTail = current.tail();
                        if (!(nextTail instanceof LinkedList<?> next)) break;
                        current = next;
                    }
                    return result;
                }),               
                new Pair<>("%", (BiFunction<Object, Object, Object>) (x, y) ->
                    ((BigInteger)x).mod((BigInteger)y)
                ),
                new Pair<>("^", (BiFunction<Object, Object, Object>) (x, y) ->
                    ((BigInteger)x).pow(((BigInteger)y).intValue())
                ),
                new Pair<>("<", (BiFunction<Object, Object, String>) (x, y) ->
                    ((BigInteger)x).compareTo((BigInteger)y) < 0 ? "#t" : "#f"
                ),
                new Pair<>(">", (BiFunction<Object, Object, String>) (x, y) ->
                    ((BigInteger)x).compareTo((BigInteger)y) > 0 ? "#t" : "#f"
                ),
                new Pair<>("=", (BiFunction<Object, Object, String>) (x, y) ->
                    ((BigInteger)x).equals((BigInteger)y) ? "#t" : "#f"
                ),

                new Pair<>("eq?", (Function<LinkedList<?>, Object>) (args) -> {
                    if (args == null || args.head() == null)
                        return "#f";

                    // eq? must take exactly two arguments
                    LinkedList<?> rest = (LinkedList<?>) args.tail();
                    if (rest == null || rest.head() == null)
                        throw new SyntaxException("eq? expects 2 arguments");

                    Object a = args.head();
                    Object b = rest.head();

                    if (a == b) return "#t";
                    if (a == null || b == null) return "#f";

                    if (a instanceof BigInteger && b instanceof BigInteger)
                        return ((BigInteger) a).equals(b) ? "#t" : "#f";
                    if (a instanceof String && b instanceof String)
                        return ((String) a).equals(b) ? "#t" : "#f";
                    if (a instanceof Symbol sa && b instanceof Symbol sb)
                        return sa.name.equals(sb.name) ? "#t" : "#f";
                    if (a instanceof LinkedList<?> la && b instanceof LinkedList<?> lb)
                        return la.equals(lb) ? "#t" : "#f";

                    return "#f";
                })
            );
            return environment;
        }

    private static String unescapeJava(String s) {
        // Convert common ANSI escape encodings first
        s = s.replace("\\u001b", "\u001b").replace("\\033", "\u001b");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                switch (next) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case '"' -> sb.append('"');
                    case '\'' -> sb.append('\'');
                    case '\\' -> sb.append('\\');
                    case 'u' -> {
                        // Unicode escape: \\uXXXX
                        if (i + 4 < s.length()) {
                            String hex = s.substring(i + 1, i + 5);
                            try {
                                int code = Integer.parseInt(hex, 16);
                                sb.append((char) code);
                                i += 4;
                            } catch (NumberFormatException e) {
                                sb.append("\\u").append(hex);
                            }
                        } else {
                            sb.append("\\u");
                        }
                    }
                    default -> sb.append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

}

