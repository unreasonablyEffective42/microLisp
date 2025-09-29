import java.util.ArrayList;
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
    Evaluator() {}
    //Gets the various builtIn operations from the corresponding string
    private static BinaryOperator<Integer> getPrimitive(String op){
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
    private static boolean isNumber (Token token){
        return token.type().equals("NUMBER");
    }
    private static boolean isString (Token token){
        return token.type().equals("STRING");
    }
    private static boolean isSymbol (Token token){
        return token.type().equals("SYMBOL");
    }
    private static boolean isLambda (Token token){
        return token.type().equals("LAMBDA");
    }
    private static boolean isCond (Token token){
        return token.type().equals("COND");
    }
    private static boolean isQuote (Token token){
        return token.type().equals("QUOTE");
    }
    private static boolean isBool (Token token){
        return token.type().equals("BOOLEAN");
    }
    private static boolean isPrimitive (Token token){
        return token.type().equals("PRIMITIVE");
    }
    private static boolean isClosure (Token token){
        return token.type().equals("CLOSURE");
    }
    private static Object evaluateCond (ArrayList<Node<Token>> conditions, Environment environment){
        return null;
    }
    private static Object evaluateList (ArrayList<Node<Token>> list, Environment environment){
        ArrayList<Object> res = new ArrayList<>(list);
        for(Node<Token> node : list){
            res.add(eval(node, environment));
        }
        return res;
    }

    public static Object eval(Node<Token> expression, Environment environment){
        if (isNumber(expression.value) || isBool(expression.value) || isString(expression.value)) {
            return expression.value.value();
        }
        else if (isSymbol(expression.value)) {
            return environment.lookup((String) expression.value.value())
                    .orElseThrow(() -> new RuntimeException("Unbound symbol"));
        }
        else if (isLambda(expression.value)){

            ArrayList<Token> args = new ArrayList<>();
            args.add(new Token<>("VARS",expression.children.get(0)));
            args.add(new Token<>("BODY",expression.children.subList(1, expression.children.size()-1)));
            args.add(new Token<>("ENV", environment));
            Token lambda = new Token<>("CLOSURE", args);
            return lambda;
        }
        else if (isCond(expression.value)){
            return evaluateCond(expression.children, environment);
        }
        else if (isQuote(expression.value)){
            return expression.value.value();
        }
        else {
            return applyProcedure((Token<String, ArrayList>) expression.value, evaluateList(expression.children, environment));
        }
    }

    private static Object applyProcedure(Token<String, ArrayList> procedure, ArrayList<Token> arguments) {
        if (isPrimitive(procedure)){
            return applyPrimitive(String.valueOf(procedure.value()), arguments);
        }
        else if (isClosure(procedure)){
            return eval(procedure.value().get(1), bind(procedure.value().get(0)), arguments ,procedure.value().get(2));
        }
        else {
            throw new SyntaxException("First argument to apply is not a procedure " + procedure);
        }
    }

    private static Object applyPrimitive(String procedure, ArrayList<Object> arguments) {
        try{
            Object res = evaluateList(arguments).stream().reduce(getPrimitive(procedure));
            return res;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
