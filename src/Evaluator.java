import java.util.ArrayList;
import java.util.Optional;
import java.util.function.BinaryOperator;
/*
The evaluator is a static class that actually comprises two mutually recursive operations. EVAL will take an expression(a tree, or a label)
and walk down the tree to see what can be evaluated, when a label is found at the head of a node, it will see if it
is an operation, or a function in the environment, if it is just a label it will return its value if it has one.
If so, it will try to APPLY the operation or function to the children of the current root node. If APPLY, during the
attempt at application of the children to the operations, finds sub expressions of tree or label types, will EVAL those,
which will eventually return an applicable type. When all the children of the node are in applicable type, Apply will
do a fold operation over the values of the child nodes and then return its value back up the calling EVAL
 */
public class Evaluator {
    private static BinaryOperator<Integer> getOperation(String op){
        return switch (op) {
            case "PLUS" -> (x, y) -> x + y;
            case "MINUS" -> (x, y) -> x - y;
            case "MULTIPLY" -> (x, y) -> x * y;
            case "DIVIDE" -> (x, y) -> x / y;
            case "MODULO" -> (x, y) -> x % y;
            case "EXPONENT" -> (x, y) -> x ^ y;
            default -> throw new IllegalStateException("Unexpected value: " + op);
        };
    }

    public static int apply(Node <String> expr){
        String operation = expr.value;
        BinaryOperator<Integer>op = getOperation(operation);
        ArrayList<Integer> childrenValues = new ArrayList<Integer>();
        for (Node<String>child : expr.children){
            childrenValues.add(Integer.valueOf(child.value));
        }
        Optional<Integer> result = childrenValues.stream().reduce(op);
        return result.get();
    }
}
