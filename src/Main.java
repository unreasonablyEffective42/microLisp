public class Main {

    public static void main(String[] args){
        String src = "(number? #\\strings \"blah blah blah\")";
        //String src = "(foo (* 2 (+ 3 1) (+ #t 5)))";
        //String src = "(* 1 2 3 4 5 6)";
        Dict environment = new Dict();
        Parser parser = new Parser(src);
        Tree<Node<Token>> parseTree = new Tree<Node<Token>>(parser.parse());
        parseTree.root.printNodes();

    }



}
