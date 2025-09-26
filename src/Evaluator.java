import java.util.ArrayList;
import java.util.Optional;
import java.util.function.BinaryOperator;

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
