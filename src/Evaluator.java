import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

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
    private static boolean isList      (Token<?, ?> t){ return isType(t, "LIST"); }
    private static boolean isCond      (Token<?, ?> t){ return isType(t, "COND"); }
    private static boolean isQuote     (Token<?, ?> t){ return isType(t, "QUOTE"); }
    private static boolean isBool      (Token<?, ?> t){ return isType(t, "BOOLEAN"); }
    private static boolean isPrimitive (Token<?, ?> t){ return isType(t, "PRIMITIVE"); }
    private static boolean isClosure   (Token<?, ?> t){ return isType(t, "CLOSURE"); }
    private static boolean isDefine    (Token<?, ?> t){ return isType(t, "DEFINE"); }
    private static boolean isNull      (Token<?, ?> t){ return isType(t, "NULL"); }
    private static boolean isAtom      (Token<?, ?> t){ return isType(t, "NUMBER") || isType(t, "BOOLEAN") ; }
    // ---------- primitive table ----------
   
    private static BiFunction<Object, Object, Object> getPrimitive(String op) {
        return switch (op) {
            case "PLUS" -> (x, y) -> (Integer)x + (Integer)y;
            case "MINUS" -> (x, y) -> (Integer)x - (Integer)y;
            case "MULTIPLY" -> (x, y) -> (Integer)x * (Integer)y;
            case "DIVIDE" -> (x, y) -> (Integer)x / (Integer)y;
            case "MODULO" -> (x, y) -> (Integer)x % (Integer)y;
            case "EXPONENT" -> (x, y) -> (int)Math.pow((Integer)x, (Integer)y);

            case "EQ" -> (x, y) -> x.equals(y) ? "#t" : "#f";
            case "LT" -> (x, y) -> ((Integer)x) < ((Integer)y) ? "#t" : "#f";
            case "GT" -> (x, y) -> ((Integer)x) > ((Integer)y) ? "#t" : "#f";

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
    private static Object evaluateCond(ArrayList<Node<Token>> clauses, Environment env) {
        for (Node<Token> clause : clauses) {
            Node<Token> predicate = clause.getChildren().get(0);
            Node<Token> body      = clause.getChildren().get(1);

            Object result = eval(predicate, env);
            if ("#t".equals(result)) {
                return eval(body, env);
            }
        }
        return null; // or raise error if no clause matches
    }
    // Recursively convert quoted nodes into raw values or LinkedLists
    private static Object quoteToValue(Node<Token> node) {
        Token<?,?> tok = node.getValue();
        if (isList(tok)) {
            ArrayList<Object> elems = new ArrayList<>();
            for (Node<Token> child : node.getChildren()) {
                elems.add(quoteToValue(child));
            }
            return new LinkedList<>(elems);
        } else {
            return tok.value();  // atom: number, symbol, string, boolean
        }
    }

    // Evaluate all body forms in order and return the last one
    // This is optional Scheme-style sequencing.
    private static Object evalSequence(Node<Token> body, Environment env) {
        if (body.getChildren().isEmpty()) {
            return eval(body, env);
        }
        Object result = null;
        for (Node<Token> expr : body.getChildren()) {
            result = eval(expr, env);
        }
        return result;
    }
    // ---------- core eval ----------

  
public static Object eval(Node<Token> expr, Environment env){
    Token<?,?> t = expr.getValue();

    // Atoms
    if (isNumber(t) || isBool(t) || isNull(t)) return t.value();

    // Strings -> LinkedList of chars
    if (isString(t)) {
        String str = (String) t.value();
        ArrayList<Object> chars = new ArrayList<>();
        for (int i = 0; i < str.length(); i++) chars.add(String.valueOf(str.charAt(i)));
        return new LinkedList<>(chars);
    }

    // Quote
    if (isQuote(t)) {
        if (expr.getChildren().size() != 1)
            throw new SyntaxException("Quote only accepts one argument");
        Node<Token> quoted = expr.getChildren().get(0);
        if (isList(quoted.getValue())) {
            ArrayList<Object> elems = new ArrayList<>();
            for (Node<Token> c : quoted.getChildren()) elems.add(quoteToValue(c));
            return new LinkedList<>(elems);
        }
        return quoted.getValue().value();
    }

    // Define
    if (isDefine(t)) {
        String label = (String) expr.getChildren().get(0).getValue().value();
        Object binding = eval(expr.getChildren().get(1), env);
        env.addFrame(new Pair<>(label, binding));
        return label; // nicer REPL output
    }

    // Cond
    if (isCond(t)) return evaluateCond(expr.getChildren(), env);

    // Lambda form → build closure token
    if (isLambda(t)) {
        ArrayList<Node<Token>> kids = expr.getChildren();
        ArrayList<Token> closure = new ArrayList<>();
        @SuppressWarnings("unchecked")
        ArrayList<Node<Token>> params = kids.get(0).getChildren();
        closure.add(new Token<>("VARS", params));
        closure.add(new Token<>("BODY", kids.get(1)));
        closure.add(new Token<>("ENV", env));
        Token<String,Object> closureTok = new Token<>("CLOSURE", closure);

        // IIFE case
        if (kids.size() > 2) {
            ArrayList<Object> args = new ArrayList<>();
            for (int i = 2; i < kids.size(); i++) args.add(eval(kids.get(i), env));
            return applyProcedure(closureTok, args);
        }
        return closureTok;
    }

    // --- KEY FIX SECTION ---
    // Application form: (f arg1 arg2 ...)
    if (isList(t) && !expr.getChildren().isEmpty()) {
        // evaluate the operator (could itself be a lambda expression)
        Object op = eval(expr.getChildren().get(0), env);

        // evaluate arguments
        ArrayList<Object> args = new ArrayList<>();
        for (int i = 1; i < expr.getChildren().size(); i++)
            args.add(eval(expr.getChildren().get(i), env));

        // now apply if callable
        if (op instanceof Token<?,?> tok) {
            @SuppressWarnings("unchecked")
            Token<String,Object> proc = (Token<String,Object>) tok;
            return applyProcedure(proc, args);
        } else if (op instanceof Function<?,?> fn) {
            @SuppressWarnings("unchecked")
            Function<Object,Object> f = (Function<Object,Object>) fn;
            if (args.size() != 1)
                throw new SyntaxException("Function expects 1 arg, got " + args.size());
            return f.apply(args.get(0));
        } else if (op instanceof BiFunction<?,?,?> bfn) {
            @SuppressWarnings("unchecked")
            BiFunction<Object,Object,Object> bf = (BiFunction<Object,Object,Object>) bfn;
            if (args.size() != 2)
                throw new SyntaxException("BiFunction expects 2 args, got " + args.size());
            return bf.apply(args.get(0), args.get(1));
        } else {
            return op; // literal or data list
        }
    }

    // Primitives
    if (isPrimitive(t)) {
        ArrayList<Object> args = new ArrayList<>();
        for (Node<Token> c : expr.getChildren()) args.add(eval(c, env));
        @SuppressWarnings("unchecked")
        Token<String,Object> proc = (Token<String,Object>) t;
        return applyProcedure(proc, args);
    }

    // Symbol lookup
    if (isSymbol(t) && expr.getChildren().isEmpty()) {
        String sym = (String) t.value();
        return env.lookup(sym).orElseThrow(() -> new RuntimeException("Unbound symbol: " + sym));
    }

    throw new SyntaxException("Cannot evaluate expression with head: " + t);
}


    private static void bind (ArrayList<Node<Token>> vars, ArrayList<Object> args, Environment env){
        if (vars.size() == args.size()) {
            List<Pair<String, Object>> bindings = new ArrayList<>();
            for (int i = 0; i < vars.size(); i++) {
                Node<Token> var = vars.get(i);
                Object arg = args.get(i);
                // Extract the symbol name from the Token
                Token<?,?> varTok = var.getValue();
                String name = (String) varTok.value();      // <— was var.value.toString()
                bindings.add(new Pair<>(name, arg));
            }
            env.addFrame(bindings);
        } else  {
            throw new IllegalStateException("Variable count mismatch");
        }
    }

    public static Object applyProcedure(Token<String,Object> proc, ArrayList<Object> args) {
        if (isPrimitive(proc)) {
            String opName = (String) proc.value();
            return applyPrimitive(opName, args);
        }
        else if (isClosure(proc)) {
            @SuppressWarnings("unchecked")
            ArrayList<Token> closureParts = (ArrayList<Token>) proc.value();

            @SuppressWarnings("unchecked")
            ArrayList<Node<Token>> params = (ArrayList<Node<Token>>) closureParts.get(0).value();
            Node<Token> body = (Node<Token>) closureParts.get(1).value();
            Environment capturedEnv = (Environment) closureParts.get(2).value();

            // New environment frame layered on captured environment
            Environment newEnv = new Environment();
            newEnv.frames.addAll(capturedEnv.frames);

            // Normalize arguments: evaluate nodes if needed
            ArrayList<Object> normalizedArgs = new ArrayList<>();
            for (Object a : args) {
                if (a instanceof Node) {
                    @SuppressWarnings("unchecked")
                    Node<Token> n = (Node<Token>) a;
                    normalizedArgs.add(eval(n, newEnv));
                } else {
                    normalizedArgs.add(a);
                }
            }

            // Ensure param count matches arg count
            if (params.size() != normalizedArgs.size()) {
                throw new IllegalStateException(
                    "Variable count mismatch: expected " + params.size() + " but got " + normalizedArgs.size());
            }

            // Bind args to params in new frame
            bind(params, normalizedArgs, newEnv);

            // Evaluate body in extended env
            
            // Evaluate body (supports multi-expression lambdas if desired)
            return evalSequence(body, newEnv);

        }
        else {
            throw new SyntaxException("First position is not a procedure: " + proc);
        }
    }


    // Apply a primitive procedure (+, -, *, /, etc.)
    private static Object applyPrimitive(String opName, ArrayList<Object> args) {
        BiFunction<Object,Object,Object> op = getPrimitive(opName);

        if (args.isEmpty()) {
            // Identity for +, 0; for *, 1 (depends on your getPrimitive design)
            if (opName.equals("+")) return 0;
            if (opName.equals("*")) return 1;
            throw new RuntimeException("Operator " + opName + " needs at least one argument");
        }

        if (args.size() == 1) {
            return args.get(0);
        } else if (args.size() == 2){
            return op.apply((Object) args.get(0),(Object) args.get(1)); 
        } else {       
            Object acc = args.get(0);
            for (int i = 1; i < args.size(); i++) {
                acc = op.apply((Integer) acc, (Integer) args.get(i));
            }
            return acc;
        }
    }
}
