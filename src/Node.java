import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;

public class Node<T> {
    T value;
    ArrayList<Node<T>> children = new ArrayList<>();

    @SafeVarargs
    public Node(T value, Node<T>... children_) {
        this.value = value;
        Collections.addAll(this.children, children_);
    }
    //constructor for leaf node
    public Node(T value){
        this.value = value;
    }

    public T getValue() {
        return value;
    }
    public void setValue(T value) {
        this.value = value;
    }
    public ArrayList<Node<T>> getChildren() {
        return children;
    }
    public void addChild(Node<T> child) {
        children.add(child);
    }
    public void removeChild(Node<T> child) {
        children.remove(child);
    }

    @SafeVarargs
    public final void addChildren(Node<T>... children_) {
        Collections.addAll(children, children_);
    }

    public void createChild(T value){
        Node<T> child = new Node<>(value);
        children.add(child);
    }

    @SafeVarargs
    public final void createChildren(T... values){
        for (T value: values){
            this.createChild(value);
        }
    }
    public void printNodes(){
        System.out.println(value);
        for(Node<T> child : children){
            child.printNodes();
        }
    }

    public void createChildren(List<Object> elems) {
        for (Object value: elems){
            this.createChild((T)value);
        }
    }

    public boolean isLeaf(){
        return children.isEmpty();
    }
}
