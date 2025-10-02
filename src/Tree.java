import java.util.stream.Stream;
//basic tree class
@SuppressWarnings("rawtypes")
public class Tree<T extends Comparable<T>> {
    Node<Token> root;

    public Tree(Node<Token> root_) {
        root = root_;
    }

    public Tree(){
        root = null;
    }

    public void printTree() {
        root.printNodes(0);
    }
}
