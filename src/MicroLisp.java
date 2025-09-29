import java.util.Scanner;
import java.util.function.Function;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
public class MicroLisp {
    public static void main(String[] args){
        Token eof = new Token<>("EOF","EOF");
        Environment environment = new Environment(
                new Pair<>("even?",(Function<Integer, String>) (x) -> x % 2 == 0 ? "#t" : "#f"),
                new Pair<>("odd?",(Function<Integer, String>) (x) -> x % 2 == 0 ? "#f" : "#t")
                //new Pair<String,Object>("eq?",(x,y) -> x == y)
        );
        String src;
        if (args.length == 1){
            try {
                src = Files.readString(Path.of(args[0]));
                Parser parser = new Parser(src);
                Node current = parser.parse();
                while(!current.value.equals(eof)){
                    Evaluator.eval(current, environment);
                    current = parser.parse();
                }
                repl(environment);
            }
            catch (IOException e){}
        }
        else {
            repl(environment);
        }


    }
    static void repl(Environment environment){
        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.print(">>>");
            String input = scanner.nextLine();
            if(input.equals(":exit")){
                break;
            }
            Parser p = new Parser(input);
            Object result = Evaluator.eval(p.parse(),environment);
            System.out.println(result.toString());
        }
    }



}
