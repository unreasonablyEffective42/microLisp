public class Main {

    public static void main(String[] args){
        //String src = "(number? #\\strings \"blah blah blah\" 2)";
        String src = "(foo (* 2 (+ 3 1) (+ #t 5)))";
        //String src = "(* 1 2 3 4 5 6)";
        Environment environment = new Environment(
                new Pair<String,Object>("x",3),
                new Pair<String,Object>("y",4)
        );
        Parser parser = new Parser(src);
        Tree<Node<Token>> parseTree = new Tree<>(parser.parse());
        parseTree.root.printNodes();

    }



}
