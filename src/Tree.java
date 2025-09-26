import java.util.stream.Stream;
//basic tree class
public class Tree<T extends Comparable<T>> {
    Node<T> root;

    public Tree(Node<T> root_) {
        root = root_;
    }

    public Tree(){
        root = null;
    }

    public void printTree() {
        root.printNodes();
    }
}
