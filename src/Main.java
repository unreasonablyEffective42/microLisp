import java.util.Scanner;
public class Main {
    public static void main(String[] args){
        Environment environment = new Environment(
                new Pair<String,Object>("x",3),
                new Pair<String,Object>("y",4)
                //new Pair<String,Object>("eq?",(x,y) -> x == y)
        );

        Main.repl(environment);

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
