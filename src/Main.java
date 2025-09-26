public class Main {

    public static void main(String[] args){
        String src = "(+ #\\strings 2)";
        //String src = "(foo (* 2 (+ 3 1) (+ #t 5)))";
        //String src = "(* 1 2 3 4 5 6)";


        Parser parser = new Parser(src);
        Tree<String> parseTree = new Tree<>(parser.parse());
        parseTree.root.printNodes();

    }



}
