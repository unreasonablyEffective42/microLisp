import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
This is a custom generic Node type, for creating tree like structures. Any value can be used for the value field,
and it supports an arbitrary number of children. The children are currently stored as an ArrayList, but will be updated
to a custom List type
 */

public class Node<T> {
    T value;
    ArrayList<Node<T>> children = new ArrayList<>();

    @SafeVarargs
    //constructor for Nodes with variable number of children
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
    //returns all child nodes
    public ArrayList<Node<T>> getChildren() {
        return children;
    }
    //add an existing node as a child of this node
    public void addChild(Node<T> child) {
        children.add(child);
    }
    //to be implemented
    public void removeChild(Node<T> child) {
        children.remove(child);
    }
    //Add multiple nodes at once as child nodes
    @SafeVarargs
    public final void addChildren(Node<T>... children_) {
        Collections.addAll(children, children_);
    }

    //create a child node from a value
    public void createChild(T value){
        Node<T> child = new Node<>(value);
        children.add(child);
    }
    //create multiple child nodes from values
    @SafeVarargs
    public final void createChildren(T... values){
        for (T value: values){
            this.createChild(value);
        }
    }
    //for debugging, traverses the tree and prints all values
    public void printNodes(){
        System.out.println(value);
        for(Node<T> child : children){
            child.printNodes();
        }
    }
    //If a node has no children, it is a leaf
    public boolean isLeaf(){
        return children.isEmpty();
    }
}
