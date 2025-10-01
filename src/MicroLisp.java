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
                //new Pair<>("null?",(Function<Object,String>) (x) -> "()".equals(x.toString()) ? "#t" : "#f"),
                new Pair<>("even?",(Function<Integer, String>) (x) -> x % 2 == 0 ? "#t" : "#f"),
                new Pair<>("odd?",(Function<Integer, String>) (x) -> x % 2 == 0 ? "#f" : "#t"),
                new Pair<>("head",(Function<LinkedList, Object>) (lst) -> lst.head()),
                new Pair<>("tail",(Function<LinkedList, Object>) (lst) -> lst.tail()),
                new Pair<>("print",(Function<Object,Object>) x1 -> {
                    System.out.println(x1);
                    return "IO::()";
                }),
                new Pair<>("null?", (Function<Object,String>) (x) -> {
                    if (x == null) return "#t";                     // treat Java null as empty
                    if (x instanceof LinkedList<?> l && l.isEmpty()) return "#t";
                    if (x.toString().equals("()")) return "#t";
                    return "#f";
                })
                /*
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
                })*/
        );
        environment.addFrame(
          new Pair<>("eval", (Function<Object,Object>) (str) -> {
            Parser p = new Parser(str.toString());
            return Evaluator.eval(p.parse(), environment);
          }),

          new Pair<>("cons", (BiFunction<Object,Object,LinkedList>) (fst, snd) -> {
              if (fst == null || "()" .equals(fst.toString())) {
                  throw new SyntaxException("First element of a pair cannot be null");
              }
              // Proper list tail: next cell
              if (snd instanceof LinkedList<?> tailList) {
                  @SuppressWarnings("unchecked")
                  LinkedList<Object> properTail = (LinkedList<Object>) (LinkedList<?>) tailList;
                  return new LinkedList<>(fst, properTail);
              }

              // Empty list tail
              if (snd == null || "()" .equals(snd.toString())) {
                  return new LinkedList<>(fst, (LinkedList<Object>) null);
              }

              // Improper list tail (dotted pair)
              return new LinkedList<>(fst, snd);
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
