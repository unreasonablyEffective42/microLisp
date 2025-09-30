import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
public class MicroLisp {
    public static void main(String[] args){
        Token eof = new Token<>("EOF","EOF");
        Environment environment = new Environment(
                new Pair<>("else", "#t"),
                new Pair<>("even?",(Function<Integer, String>) (x) -> x % 2 == 0 ? "#t" : "#f"),
                new Pair<>("odd?",(Function<Integer, String>) (x) -> x % 2 == 0 ? "#f" : "#t"),
                new Pair<>("print",(Function<Object,Object>) x1 -> {
                   System.out.println(x1);
                    return "IO::()";
                })
        );
        environment.addFrame(
                new Pair<>("eval", (Function<Object,Object>) (str) -> {
                    Parser p = new Parser(str.toString());
                    return Evaluator.eval(p.parse(), environment);
                })
        );

        String src;
        if (args.length > 0){
            try {
                src = Files.readString(Path.of(args[0]));
                Parser parser = new Parser(src);
                Node current = parser.parse();
                while(!((Token) current.value).type().equals("EOF")){
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
