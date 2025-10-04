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
    private static boolean isDo        (Token<?, ?> t){ return isType(t, "DO"); }
    private static boolean isLet       (Token<?, ?> t){ return isType(t, "LET"); }
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
    private static Object evaluateDo(ArrayList<Node<Token>> todos, Environment env){ 
        if (todos.isEmpty()) return null;
        for (int i = 0; i < todos.size() - 1; i++){ 
            eval(todos.get(i),env); 
        } 
        return eval(todos.get(todos.size()-1),env); 
    }
    
    
    
    private static Object evaluateLet(ArrayList<Node<Token>> let, Environment env){
        if (let.size() != 2) {
            throw new SyntaxException("let requires a binding list and one body expression");
        }

        Node<Token> bindingsNode = let.get(0);
        Node<Token> bodyNode = let.get(1);

        ArrayList<Node<Token>> bindingPairs = bindingsNode.getChildren();
        if (bindingPairs.isEmpty()) {
            return eval(bodyNode, env);
        }

        // --- Build a lambda that takes all bindings as parameters ---
        Node<Token> lambdaNode = new Node<>(new Token<>("LAMBDA", ""));
        Node<Token> paramsNode = new Node<>(new Token<>("PARAMS", null));

        ArrayList<Object> argVals = new ArrayList<>();

        for (Node<Token> binding : bindingPairs) {
            if (binding.getChildren().size() != 2) {
                throw new SyntaxException("Each let binding must be a (symbol expr) pair");
            }

            Node<Token> labelNode = binding.getChildren().get(0);
            Node<Token> valueExpr = binding.getChildren().get(1);

            paramsNode.addChild(labelNode);
            argVals.add(eval(valueExpr, env));
        }

        lambdaNode.addChild(paramsNode);
        lambdaNode.addChild(bodyNode);

        // Evaluate the lambda to a closure
        Object closureTok = eval(lambdaNode, env);
        if (!(closureTok instanceof Token<?,?> closure)) {
            throw new SyntaxException("let expansion did not produce a closure");
        }

        @SuppressWarnings("unchecked")
        Token<String,Object> closureToken = (Token<String,Object>) closure;
        // Apply the closure to all binding values
        return applyProcedure(closureToken, argVals);
    }

    // ---------- core eval ----------
    public static Object eval(Node<Token> expr, Environment env){
        Token<?,?> t = expr.getValue();
        // Atoms
        if (isNumber(t) || isBool(t) || isNull(t)) {
            return t.value();
        }
        // Special forms

        if (isString(t)) {
            String str = (String) t.value();  
            ArrayList<Object> chars = new ArrayList<>();
            for (int i = 0; i < str.length(); i++) {
                chars.add(String.valueOf(str.charAt(i)));
            }
            return new LinkedList<>(chars);
        }
        if (isQuote(t)) {
            if (expr.getChildren().size() != 1) {
                throw new SyntaxException("Quote only accepts one argument, received: " + expr.getChildren().size() + "args: "+expr.getChildren());
            }

            Node<Token> quoted = expr.getChildren().get(0);

            if (isList(quoted.getValue())) {
                // Recursively turn children into a LinkedList of unevaluated literals
                ArrayList<Object> elems = new ArrayList<>();
                for (Node<Token> child : quoted.getChildren()) {
                    elems.add(quoteToValue(child));
                }
                return new LinkedList<>(elems);
            } else {
                return quoted.getValue().value();
            }
        }
        if (isDefine(t)){
            String label = (String)expr.getChildren().get(0).getValue().value();
            Object binding = eval(expr.getChildren().get(1), env);
            env.addFrame(new Pair<>(label, binding));
            return env;
        }
        if (isCond(t)) {
            return evaluateCond(expr.getChildren(), env);
        }

        if (isList(t)) {
            ArrayList<Node<Token>> kids = expr.getChildren();
            int dot = -1;
            for (int i = 0; i < kids.size(); i++) {
                if ("DOT".equals(kids.get(i).getValue().type())) { dot = i; break; }
            }

            if (dot >= 0) {
                if (dot + 1 >= kids.size()) throw new SyntaxException("Dot without following cdr expression");
                Object cdr = eval(kids.get(dot + 1), env);

                Object cell = cdr;
                for (int i = dot - 1; i >= 0; i--) {
                    cell = (cell instanceof LinkedList)
                        ? new LinkedList<>(eval(kids.get(i), env), (LinkedList<?>) cell)
                        : new LinkedList<>(eval(kids.get(i), env), cell); // improper tail stays raw
                }
                return cell; // the full (possibly improper) list
            }

            // proper list case unchanged
            ArrayList<Object> elems = evaluateList(expr.getChildren(), env);
            return new LinkedList<>(elems);
        }
        if (isDo(t)){ 
            if (expr.getChildren().size() < 1){ 
                throw new SyntaxException("Do blocks require at least one expression"); 
            } return evaluateDo(expr.getChildren(),env); 
        }
        if (isLet(t)){
            return evaluateLet(expr.getChildren(),env);
        }
        if (isLambda(t)) {
            // Expect children: [PARAMS, BODY, ...maybe args for IIFE...]
            ArrayList<Node<Token>> children = expr.getChildren();
            // Build closure (params list is first child, body is second)
            ArrayList<Token> closureParts = new ArrayList<>();
            @SuppressWarnings("unchecked")
            ArrayList<Node<Token>> params = children.get(0).getChildren(); // "PARAMS" node’s children
            closureParts.add(new Token<>("VARS", params));
            Node<Token> body = children.get(1);
            closureParts.add(new Token<>("BODY", body));
            closureParts.add(new Token<>("ENV", env));
            Token<String,Object> closureTok = new Token<>("CLOSURE", closureParts);
            // If it's an IIFE, apply to remaining args
            if (children.size() > 2) {
                ArrayList<Object> argVals = new ArrayList<>();
                for (int i = 2; i < children.size(); i++) {
                    argVals.add(eval(children.get(i), env));
                }
                return applyProcedure(closureTok, argVals);
            }
            // otherwise just the closure literal
            return closureTok;
        }
        // Variable (symbol) — atom vs call
        if (isSymbol(t)) {
            if (expr.getChildren().isEmpty()) {
                String sym = (String) t.value();
                return env.lookup(sym).orElseThrow(() -> new RuntimeException("Unbound symbol: " + sym));
            }
            // (symbol arg1 arg2 ...)
            String sym = (String) t.value();
            Object op = env.lookup(sym).orElseThrow(() -> new RuntimeException("Unbound symbol: " + sym));

            ArrayList<Object> argVals = new ArrayList<>();
            for (Node<Token> child : expr.getChildren()) {
                argVals.add(eval(child, env));
            }
            if (op instanceof Token<?,?> opTok) {
                @SuppressWarnings("unchecked")
                Token<String,Object> procTok = (Token<String,Object>) opTok;
                return applyProcedure(procTok, argVals);
            } else if (op instanceof Function<?,?> fn) {
                if (argVals.size() != 1)
                    throw new SyntaxException("Procedure " + sym + " expects 1 argument, got " + argVals.size());
                @SuppressWarnings("unchecked")
                Function<Object,Object> f = (Function<Object,Object>) fn;
                return f.apply(argVals.get(0));
            } else if (op instanceof BiFunction<?,?,?> bfn) {
                if (argVals.size() != 2)
                    throw new SyntaxException("Procedure " + sym + " expects 2 arguments, got " + argVals.size());
                @SuppressWarnings("unchecked")
                BiFunction<Object,Object,Object> bf = (BiFunction<Object,Object,Object>) bfn;
                return bf.apply(argVals.get(0), argVals.get(1));
            } else {
                throw new SyntaxException("First position is not a procedure: " + sym);
            }
        }
        // Primitive-headed application: (+ 1 2), (< 3 4), etc.
        if (isPrimitive(t)){
            ArrayList<Object> argVals = new ArrayList<>();
            for (Node<Token> child : expr.getChildren()) {
                argVals.add(eval(child, env));
            }
            @SuppressWarnings("unchecked")
            Token<String,Object> procTok = (Token<String,Object>) t;
            return applyProcedure(procTok, argVals);
        }
        if (!expr.getChildren().isEmpty()) {
            Object headVal = eval(new Node<>(t), env);  // evaluate head
            ArrayList<Object> argVals = new ArrayList<>();
            for (Node<Token> child : expr.getChildren()) {
                argVals.add(eval(child, env));
            }

            if (headVal instanceof Token<?,?> headTok) {
                @SuppressWarnings("unchecked")
                Token<String,Object> procTok = (Token<String,Object>) headTok;
                return applyProcedure(procTok, argVals);
            }
        }
        // If nothing above matched, this should be an application form we don't recognize
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
            return eval(body, newEnv);
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
