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
        Environment environment = GlobalEnvironment.initGlobalEnvironment();
        Vector.addVectorEnv(environment);
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
            if (args.length > 1){
                environment = loadOnStart(environment, args,1);
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
                environment = loadOnStart(environment,args,0);
                try {
                    Parser m = new Parser("(main)");
                    Evaluator.eval(m.parse(),environment);
                }
                catch (RuntimeException e){
                    System.out.println("main not found");
                }
            }
            else{
                System.out.println("No files passed in non-interactive-mode");
            }

        }
    }
    static Environment loadOnStart(Environment environment,String[] args,int start){
        Token eof = new Token<>("EOF","EOF");
        String src;  
        for (int i=start;i<args.length;i++){
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
        return environment;
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
