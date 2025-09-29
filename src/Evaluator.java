import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

/*
 The evaluator walks the AST and evaluates expressions. This version focuses on
 getting primitive application working cleanly and predictably.

 Eval takes in an expression and attempts to reduce it, either returning values, looking up symbol values \
 in the environment or calling apply to an expression and some arguments
 Notes:
 - Primitives are sourced from PRIMITIVE tokens (Lexer emits PRIMITIVE type).
 - Symbols are looked up in the Environment and unwrapped.
 - (quote ...) returns the raw value without evaluation.
 - (cond ...) and (lambda ...) are to be implemented
*/
public class Evaluator {
    Evaluator() {}

    // ---------- token helpers ----------
    private static boolean isType      (Token<?, ?> t, String ty) { return ty.equals(t.type()); }
    private static boolean isNumber    (Token<?, ?> t){ return isType(t, "NUMBER"); }
    private static boolean isString    (Token<?, ?> t){ return isType(t, "STRING"); }
    private static boolean isSymbol    (Token<?, ?> t){ return isType(t, "SYMBOL"); }
    private static boolean isLambda    (Token<?, ?> t){ return isType(t, "LAMBDA"); }
    private static boolean isCond      (Token<?, ?> t){ return isType(t, "COND"); }
    private static boolean isQuote     (Token<?, ?> t){ return isType(t, "QUOTE"); }
    private static boolean isBool      (Token<?, ?> t){ return isType(t, "BOOLEAN"); }
    private static boolean isPrimitive (Token<?, ?> t){ return isType(t, "PRIMITIVE"); }
    private static boolean isClosure   (Token<?, ?> t){ return isType(t, "CLOSURE"); }
    private static boolean isDefine    (Token<?, ?> t){ return isType(t, "DEFINE"); }

    // ---------- primitive table ----------
    private static BiFunction<Integer,Integer,Object> getPrimitive(String op){
        return switch (op) {
            case "PLUS"      -> (x, y) -> x + y;
            case "MINUS"     -> (x, y) -> x - y;
            case "MULTIPLY"  -> (x, y) -> x * y;
            case "DIVIDE"    -> (x, y) -> x / y;
            case "MODULO"    -> (x, y) -> x % y;
            case "EXPONENT"  -> (x, y) -> (int)Math.pow(x, y);
            case "EQ"    -> (x, y) -> x.equals(y) ? "#t" : "#f";
            case "LT"        -> (x, y) -> x < y ? "#t" : "#f";
            case "GT"        -> (x, y) -> x > y ? "#t" : "#f";
            default -> throw new IllegalStateException("Unexpected operator: " + op);
        };
    }
    // ---------- list evaluation ----------
    private static ArrayList<Object> evaluateList(ArrayList<Node<Token>> list, Environment env){
        ArrayList<Object> out = new ArrayList<>(list.size());
        for (Node<Token> node : list) {
            out.add(eval(node, env));
        }
        return out;
    }

    // ---------- special forms ----------
    private static Object evaluateCond(ArrayList<Node<Token>> clauses, Environment env){
        // TODO: implement cond once parser builds (test expr) pairs per clause.
        // Placeholder: return null for now to keep the evaluator compiling.
        return null;
    }

    // ---------- core eval ----------
    public static Object eval(Node<Token> expr, Environment env){
        Token<?,?> t = expr.getValue();

        if (isNumber(t) || isBool(t) || isString(t)) {
            return t.value();
        }
        else if (isSymbol(t)){
            String sym = (String) t.value();
            return env.lookup(sym).orElseThrow(() -> new RuntimeException("Unbound symbol: " + sym));
        }
        else if (isLambda(t)){
            // TODO: Build and return a CLOSURE token here
            // (params list, body nodes, and captured env). For now, just return the raw token.
            return t;
        }
        else if (isCond(t)){
            return evaluateCond(expr.getChildren(), env);
        }
        else if (isQuote(t)){
            // In classic Lisp, (quote x) just returns the unevaluated datum.
            return t.value();
        }
        else if (isDefine(t)){
            String label = (String)expr.children.get(0).value.value();
            Object binding = eval(expr.children.get(1), env);
            env.addFrame(new Pair<>(label, binding));
            return env;
        }
        else {
            // Application: head token is operator or procedure; children are args
            ArrayList<Object> argVals = evaluateList(expr.getChildren(), env);
            return applyProcedure(t, argVals);
        }
    }

    // ---------- application ----------
    private static Object applyProcedure(Token<?,?> proc, ArrayList<Object> args) {
        if (isPrimitive(proc)){
            String opName = (String) proc.value();
            return applyPrimitive(opName, args);
        }
        else if (isClosure(proc)){
            // TODO: unpack closure payload (params, body, env), extend env with arg bindings,
            // then eval body in that env. This requires Environment to support pushing a new frame.
            throw new UnsupportedOperationException("Closure application not implemented yet");
        }
        else {
            throw new SyntaxException("First position is not a procedure: " + proc);
        }
    }

    private static Object applyPrimitive(String opName, ArrayList<Object> args) {
        if (args.size() < 2) throw new RuntimeException("Expected at least two arguments for: " + opName);
        BiFunction<Integer,Integer,Object> op = getPrimitive(opName);

        Object acc = args.get(0);
        for (int i = 1; i < args.size(); i++) {
            acc = op.apply((Integer) acc, (Integer) args.get(i));
        }
        return acc;
    }
}
