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
                new Pair<>("head",(Function<LinkedList, Object>) (lst) -> lst.head()),
                new Pair<>("tail",(Function<LinkedList, Object>) (lst) -> lst.tail()),
                new Pair<>("print",(Function<Object,Object>) x1 -> {
                    System.out.println(x1);
                    return "IO::()";
                }),
                new Pair<>("map",(BiFunction<LinkedList,Function<Object,Object>,LinkedList>) (lst, fn) ->{
                  LinkedList ret = new LinkedList<>("()");
                  if (lst.size() == 0){
                    return ret;
                  }
                  ret.setHead(fn.apply(lst.head()));
                  lst=lst.tail();
                  LinkedList current = ret;
                  for (int i = 1; i < lst.size(); i++){
                    current.setTail(new LinkedList(fn.apply(lst.head())));
                    current = current.tail();
                  }
                  return ret;
                })
        );
        environment.addFrame(
          new Pair<>("eval", (Function<Object,Object>) (str) -> {
            Parser p = new Parser(str.toString());
            return Evaluator.eval(p.parse(), environment);
          }),
          new Pair<>("cons",(BiFunction<Object,Object,LinkedList>) (fst,snd) ->{
            if (fst == null || fst.toString().equals("()")){
              throw new SyntaxException("First element of a pair cannot be null");
            }
            else if (snd == null || snd.toString().equals("()")){
              Parser f = new Parser(fst.toString());
              return new LinkedList<>(Evaluator.eval(f.parse(),environment));
            }
            else {
              Parser f = new Parser(fst.toString());
              Parser s = new Parser(snd.toString());
              return new LinkedList<Object>(Evaluator.eval(f.parse(),environment),Evaluator.eval(s.parse(),environment));
            }
          })
        );

        String src;
        if (args.length > 0){
          for (int i=0;i<args.length;i++){
            try {
                src = Files.readString(Path.of(args[i]));
                Parser parser = new Parser(src);
                Node current = parser.parse();
                while(!((Token) current.value).type().equals("EOF")){
                    Evaluator.eval(current, environment);
                    current = parser.parse();
                }
                System.out.println(args[i]+ " loaded successfully");
                
            }
            catch (IOException e){
                System.out.println("Could not load file " + args[i]);
                System.out.println(e);
            }
          }
          repl(environment);
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
                System.out.println(file + " loaded successfully");
                }
              catch (IOException e){
                System.out.println("Could not load file " + input.substring(5).trim());
                System.out.println(e);
              }
                
            } else {
              Parser p = new Parser(input);
              Object result = Evaluator.eval(p.parse(),environment);
              System.out.println(result.toString());
            }
        }
    }



}
