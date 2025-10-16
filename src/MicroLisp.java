import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;
import java.math.BigInteger;
import java.util.ArrayList;

public class MicroLisp{
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String BLUE = "\u001B[38;2;45;199;193m";
    public static final String YELLOW = "\u001b[0;93m";
    public static final String ORANGE = "\u001b[38;2;255;140;0m";
    private static int debugLevel = 0;
    private static boolean interactive = false;
    private static boolean prettyprint = false;
    private static String availableFlags = "ilhp";

    public static void main(String[] args){
        // ----- Create Initial Environment --------- 
        Environment environment = makeGlobalEnv();
        FileHandling.addFileHandlingEnv(environment);
        PixelGraphics.addPixelGraphicsEnv(environment);
        // ----- decode flags ------
        if (args[0].charAt(0) == '-'){
          for (int i = 1; i < args[0].length(); i++){
            switch (args[0].charAt(i)) {
              case 'i': 
                interactive = true;
                break;
              case 'l':
                debugLevel = 1;
                break;
              case 'h':
                debugLevel = 2;
                break;
              case 'p':
                prettyprint = true;
              default:
                break;
            }       
          }
        }
        // ----- Load and display the banner and text ---------
        if (interactive) {
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
            System.out.println("\n"+YELLOW + "              MicroLisp v1.0 - (c)Jordan Jacobson 2025" + RESET);
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
            else if (prettyprint) {
                int mismatchtotal = ParenCounter.lParens(input);
                StringBuilder sb = new StringBuilder(input);
                if (mismatchtotal != 0){ 
                  ArrayList<Token> open = ParenCounter.openToks(input); 
                  int innermismatch = 0;
                  do{  
                    System.out.printf("  |" + " ".repeat(ParenCounter.indent(open)));
                    input = scanner.nextLine();
                    innermismatch = ParenCounter.lParens(input);
                    if (innermismatch > 0){
                      open.addAll(ParenCounter.openToks(input));
                      mismatchtotal += innermismatch;
                    } else if (innermismatch < 0){
                      mismatchtotal += innermismatch;
                      for (int i = innermismatch; i < 0; i++){
                        open.remove(open.size()-1);
                      }
                    } else{
                      innermismatch = 0;
                    }
                    sb.append(input);
                  }while(mismatchtotal > 0);
                }
                input = sb.toString();
                Lexer l = new Lexer(input);
                Parser p = new Parser(input);
                Node parsed = p.parse();
                Object result;
                switch (debugLevel) {
                  case 1:
                    System.out.println("DEBUG LOW\nAST:");
                    parsed.printNodes(0);
                    System.out.println("\nOUTPUT:");
                    result = Evaluator.eval(parsed, environment);
                    System.out.println(result.toString());
                    break;
                  case 2:
                    System.out.println("DEBUG HIGH\nTOKEN STREAM");
                    Token current = l.getNextToken();
                    while (!current.type().equals("EOF")) {
                      System.out.println(current);
                      current = l.getNextToken();
                    }
                    System.out.println("\nAST:");
                    parsed.printNodes(0);
                    System.out.println("\nOUTPUT:");
                    result = Evaluator.eval(parsed, environment);
                    System.out.println(result.toString());
                    break;
                  default:
                    result = Evaluator.eval(parsed, environment);
                    System.out.println(result.toString());
                    break;
                }
            } 
            else {
                int mismatchtotal = ParenCounter.lParens(input);
                StringBuilder sb = new StringBuilder(input);
                if (mismatchtotal != 0){ 
                  ArrayList<Token> open = ParenCounter.openToks(input); 
                  int innermismatch = 0;
                  do{   
                    input = scanner.nextLine();
                    innermismatch = ParenCounter.lParens(input);
                    if (innermismatch > 0){
                      open.addAll(ParenCounter.openToks(input));
                      mismatchtotal += innermismatch;
                    } else if (innermismatch < 0){
                      mismatchtotal += innermismatch;
                      for (int i = innermismatch; i < 0; i++){
                        open.remove(open.size()-1);
                      }
                    } else{
                      innermismatch = 0;
                    } 
                    sb.append('\n').append(input);
                  }while(mismatchtotal > 0);
                }
                input = sb.toString();
                Lexer l = new Lexer(input);
                Parser p = new Parser(input);
                Node parsed = p.parse();
                Object result;
                switch (debugLevel) {
                  case 1:
                    System.out.println("DEBUG LOW\nAST:");
                    parsed.printNodes(0);
                    System.out.println("\nOUTPUT:");
                    result = Evaluator.eval(parsed, environment);
                    System.out.println(result.toString());
                    break;
                  case 2:
                    System.out.println("DEBUG HIGH\nTOKEN STREAM");
                    Token current = l.getNextToken();
                    while (!current.type().equals("EOF")) {
                      System.out.println(current);
                      current = l.getNextToken();
                    }
                    System.out.println("\nAST:");
                    parsed.printNodes(0);
                    System.out.println("\nOUTPUT:");
                    result = Evaluator.eval(parsed, environment);
                    System.out.println(result.toString());
                    break;
                  default:
                    result = Evaluator.eval(parsed, environment);
                    System.out.println(result.toString());
                    break;
                }
            }
        }
    }
    public static void evalString(String src, Environment env) {
        Parser parser = new Parser(src);
        while (true) {
            Node<Token> form = parser.parse();
            Token<?,?> tok = form.getValue();
            if (tok != null && "EOF".equals(tok.type())) {
                break;
            }
            Evaluator.eval(form, env);
        }
    }

    public static Environment makeGlobalEnv() {
            Environment environment = new Environment(
                new Pair<>("else", "#t"),
                new Pair<>("null?", (Function<Object,String>) (x) -> {
                    if (x == null) return "#t";
                    if (x instanceof LinkedList<?> list) {
                        if (list.isEmpty()) return "#t";
                        if (list.isCharList() && list.size() == 0) return "#t";
                        return "#f";
                    }
                    if (x instanceof String s) {
                        return s.isEmpty() ? "#t" : "#f";
                    }
                    return "#f";
                }),                                
                new Pair<>("number?", (Function<Object,String>) (x) -> {
                    if (x instanceof Number) return "#t";
                    return "#f";
                }),
                new Pair<>("symbol?", (Function<Object,String>) (x) -> {
                    if (x instanceof Symbol) return "#t";
                    return "#f";
                }),
                new Pair<>("even?", (Function<Object, String>) (x) -> {
                    if (!(x instanceof Number n))
                        throw new RuntimeException("even?: expected number, got " + x);
                    Number remainder = Number.mod(n, Number.integer(2));
                    return Number.numericEquals(remainder, Number.zero(remainder)) ? "#t" : "#f";
                }),
                new Pair<>("odd?", (Function<Object, String>) (x) -> {
                    if (!(x instanceof Number n))
                        throw new RuntimeException("odd?: expected number, got " + x);
                    Number remainder = Number.mod(n, Number.integer(2));
                    return Number.numericEquals(remainder, Number.zero(remainder)) ? "#f" : "#t";
                }),
                new Pair<>("!",(Function<String, String>) (x) -> x.equals("#t") ? "#f" : "#t"),
                new Pair<>("and", (BiFunction<String, String, String>) (p, q) ->
                    (p.equals("#t") && q.equals("#t")) ? "#t" : "#f"
                ),
                new Pair<>("not", (Function<String,String>) (x) -> x.equals("#t") ? "#f" : "#t"),
                new Pair<>("or", (BiFunction<String, String, String>) (p, q) ->
                    (p.equals("#t") || q.equals("#t")) ? "#t" : "#f"
                ),
                new Pair<>("xor", (BiFunction<String, String, String>) (p, q) ->
                    ((p.equals("#t") && !q.equals("#t")) || (q.equals("#t") && !p.equals("#t"))) ? "#t" : "#f"
                ),
                new Pair<>("head", (Function<Object,Object>) (x) -> {
                    if (x instanceof LinkedList<?> list) {
                        Object head = list.head();
                        if (list.isCharList()) {
                            if (head == null) {
                                return LinkedList.fromString("");
                            }
                            if (head instanceof String s) {
                                return LinkedList.fromString(s);
                            }
                        }
                        return head;
                    }
                    throw new RuntimeException("head: unsupported type " + x.getClass());
                }),
                new Pair<>("tail", (Function<Object,Object>) (x) -> {
                    if (x instanceof LinkedList<?> list) {
                        Object tail = list.tail();
                        if (tail == null) {
                            if (list.isCharList()) {
                                return LinkedList.fromString("");
                            }
                            return new LinkedList<>();
                        }
                        return tail;
                    }
                    throw new RuntimeException("tail: unsupported type " + x.getClass());
                }),                
                new Pair<>("length", (Function<LinkedList, Number>) (xs) ->
                    Number.integer(xs.size())
                ),
                new Pair<>("print", (Function<Object, Object>) x1 -> {
                    if (x1 == null) {
                        System.out.println("()");
                        return LinkedList.fromString("");
                    }
                    String out;
                    if (x1 instanceof LinkedList<?> list && list.isCharList()) {
                        out = LinkedList.listToRawString(list);
                    } else {
                        out = x1.toString();
                        if (out.length() >= 2 && out.startsWith("\"") && out.endsWith("\"")) {
                            out = out.substring(1, out.length() - 1);
                        }
                    }
                    System.out.println(out);
                    return LinkedList.fromString("");
                }),
                new Pair<>("printf", (Function<Object, Object>) x1 -> {
                    if (x1 == null) return LinkedList.fromString("");
                    String out;
                    if (x1 instanceof LinkedList<?> list && list.isCharList()) {
                        out = LinkedList.listToRawString(list);
                    } else {
                        out = x1.toString();
                        if (out.length() >= 2 && out.startsWith("\"") && out.endsWith("\"")) {
                            out = out.substring(1, out.length() - 1);
                        }
                    }
                    System.out.print(unescapeJava(out));
                    return LinkedList.fromString("");
                }),
                new Pair<>("to-inexact", (Function<Number, Number>) n -> Number.toInexact(n)),
                new Pair<>("to-inexact-big", (Function<Number, Number>) n -> Number.toInexactBig(n))
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
                    if (LinkedList.isCharList(snd)) {
                        LinkedList<?> tailList = (LinkedList<?>) snd;
                        if (LinkedList.isCharList(fst)) {
                            return LinkedList.concatCharLists((LinkedList<?>) fst, tailList);
                        }
                        if (fst instanceof String s && s.length() > 0) {
                            LinkedList<String> headList = LinkedList.fromString(s);
                            return LinkedList.concatCharLists(headList, tailList);
                        }
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
                    Number sum = Number.integer(0);
                    LinkedList<?> current = args;
                    if (current == null || current.head() == null) {
                        return sum;
                    }
                    while (current != null && current.head() != null) {
                        Object head = current.head(); 
                        if (!(head instanceof Number n))
                            throw new RuntimeException("+: expected number, got " + head + " (type " + head.getClass() + ")");
                        sum = Number.add(sum, n);
                        Object tail = current.tail();
                        if (!(tail instanceof LinkedList<?> next)) break;
                        current = next;
                    }
                    return sum;
                }),
                new Pair<>("-", (Function<LinkedList<?>, Object>) (args) -> {
                    if (args == null || args.head() == null)
                        return Number.integer(0);
                    Object first = args.head();
                    if (!(first instanceof Number result))
                        throw new RuntimeException("-: expected number, got " + first);
                    Object tail = args.tail();
                    if (tail == null) {
                        return Number.sub(Number.zero(result), result);
                    }
                    if (!(tail instanceof LinkedList<?> current)) return result;
                    while (current.head() != null) {
                        Object head = current.head();
                        if (!(head instanceof Number n))
                            throw new RuntimeException("-: expected number, got " + head + " (type " + head.getClass() + ")");
                        result = Number.sub(result, n);
                        Object nextTail = current.tail();
                        if (!(nextTail instanceof LinkedList<?> next)) break;
                        current = next;
                    }
                    return result;
                }),
                new Pair<>("*", (Function<LinkedList<?>, Object>) (args) -> {
                    Number prod = Number.integer(1);
                    LinkedList<?> current = args;
                    if (current == null || current.head() == null) {
                        return prod;
                    }
                    while (current != null && current.head() != null) {
                        Object head = current.head();
                        if (!(head instanceof Number n))
                            throw new RuntimeException("*: expected number, got " + head + " (type " + head.getClass() + ")");
                        prod = Number.multiply(prod, n);
                        Object tail = current.tail();
                        if (!(tail instanceof LinkedList<?> next)) break;
                        current = next;
                    }
                    return prod;
                }),
                new Pair<>("/", (Function<LinkedList<?>, Object>) (args) -> {
                    if (args == null || args.head() == null)
                        throw new SyntaxException("/ expects at least one argument");
                    Object first = args.head();
                    if (!(first instanceof Number result))
                        throw new RuntimeException("/: expected number, got " + first + " (type " + first.getClass() + ")");
                    Object tail = args.tail();
                    if (!(tail instanceof LinkedList<?> current))
                        return result;
                    while (current.head() != null) {
                        Object head = current.head();
                        if (!(head instanceof Number n))
                            throw new RuntimeException("/: expected number, got " + head + " (type " + head.getClass() + ")");
                        result = Number.divide(result, n);
                        Object nextTail = current.tail();
                        if (!(nextTail instanceof LinkedList<?> next)) break;
                        current = next;
                    }
                    return result;
                }),               
                new Pair<>("%", (BiFunction<Object, Object, Object>) (x, y) ->
                    Number.mod((Number) x, (Number) y)
                ),
                new Pair<>("^", (BiFunction<Object, Object, Object>) (x, y) ->
                    Number.pow((Number) x, (Number) y)
                ),
                new Pair<>("<", (BiFunction<Object, Object, String>) (x, y) ->
                    Number.lessThan((Number) x, (Number) y) ? "#t" : "#f"
                ),
                new Pair<>(">", (BiFunction<Object, Object, String>) (x, y) ->
                    Number.greaterThan((Number) x, (Number) y) ? "#t" : "#f"
                ),
                new Pair<>("=", (BiFunction<Object, Object, String>) (x, y) -> {
                    if (x instanceof Number nx && y instanceof Number ny)
                        return Number.numericEquals(nx, ny) ? "#t" : "#f";
                    if (x instanceof String sx && y instanceof String sy)
                        return sx.equals(sy) ? "#t" : "#f";
                    if (x instanceof LinkedList<?> lx && y instanceof LinkedList<?> ly)
                        return lx.equals(ly) ? "#t" : "#f";
                    return "#f";
                }),

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
                    if (a instanceof Number && b instanceof Number)
                        return Number.numericEquals((Number) a, (Number) b) ? "#t" : "#f";

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
           // evalString("(define chars->string (lambda (chars) (foldl (lambda (acc ch) (cons ch acc)) \"\" (reverse chars))))", environment);
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
