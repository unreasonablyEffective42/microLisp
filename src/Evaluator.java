import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.Scanner;
import java.math.BigInteger;

/*
 The evaluator walks the AST and evaluates expressions. This version focuses on
 getting primitive application working cleanly and predictably, and adds trampolining
 for tail-call safety across lambda bodies and special forms (cond, do, lets).

 Public eval(...) is a thin wrapper over evalT(...).run(), so call-sites don't change.
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
    private static boolean isDo        (Token<?, ?> t){ return isType(t, "DO"); }
    private static boolean isLet       (Token<?, ?> t){ return isType(t, "LET"); }
    private static boolean isLets      (Token<?, ?> t){ return isType(t, "LETS"); }
    private static boolean isLetr      (Token<?, ?> t){ return isType(t, "LETR"); }
    private static boolean isLetNamed (Token<?, ?> t){ return isType(t, "LET-NAMED"); }
    private static boolean isAtom      (Token<?, ?> t){ return isType(t, "NUMBER") || isType(t, "BOOLEAN") ; }

    // ---------- primitive table ----------
    public static Object getPrimitive(String op) {
        return switch (op) {
            // ---- arithmetic ----
            case "PLUS"      -> (BiFunction<Object, Object, Object>) ((x, y) -> ((BigInteger)x).add((BigInteger)y));
            case "MINUS"     -> (BiFunction<Object, Object, Object>) ((x, y) -> ((BigInteger)x).subtract((BigInteger)y));
            case "MULTIPLY"  -> (BiFunction<Object, Object, Object>) ((x, y) -> ((BigInteger)x).multiply((BigInteger)y));
            case "DIVIDE"    -> (BiFunction<Object, Object, Object>) ((x, y) -> ((BigInteger)x).divide((BigInteger)y));
            case "MODULO"    -> (BiFunction<Object, Object, Object>) ((x, y) -> ((BigInteger)x).mod((BigInteger)y));
            case "EXPONENT" -> (BiFunction<Object, Object, Object>) ((x, y) -> {
                BigInteger base = (BigInteger) x;
                BigInteger exp  = (BigInteger) y;
                if (exp.signum() < 0) throw new IllegalArgumentException("Exponent must be non-negative");
                BigInteger result = BigInteger.ONE;
                BigInteger two = BigInteger.valueOf(2);
                while (!exp.equals(BigInteger.ZERO)) {
                    if (exp.testBit(0)) result = result.multiply(base);
                    base = base.multiply(base);
                    exp = exp.divide(two);
                }
                return result;
            });
            // ---- comparison ----
            case "LT" -> (BiFunction<Object, Object, Object>) ((x, y) ->
                ((BigInteger)x).compareTo((BigInteger)y) < 0 ? "#t" : "#f");
            case "GT" -> (BiFunction<Object, Object, Object>) ((x, y) ->
                ((BigInteger)x).compareTo((BigInteger)y) > 0 ? "#t" : "#f");

            // ---- equality ----
            case "EQ" -> (BiFunction<Object, Object, Object>) ((x, y) -> {
                if (x == null || y == null) return "#f";
                if (x instanceof BigInteger && y instanceof BigInteger)
                    return ((BigInteger)x).equals((BigInteger)y) ? "#t" : "#f";
                if (x instanceof String && y instanceof String)
                    return ((String)x).equals(y) ? "#t" : "#f";
                if (x instanceof LinkedList && y instanceof LinkedList)
                    return ((LinkedList<?>)x).equals(y) ? "#t" : "#f";
                return "#f";
            });

            // ---- zero-arg primitives ----
            case "READ" -> (Supplier<String>) () -> {
                Scanner sc = new Scanner(System.in);
                return sc.nextLine();
            };

            default -> throw new IllegalStateException("Unexpected operator: " + op);
        };
    }

    // ---------- list evaluation -------- 
    private static ArrayList<Object> evaluateList(ArrayList<Node<Token>> list, Environment env){
        ArrayList<Object> out = new ArrayList<>(list.size());
        for (Node<Token> node : list) {
            out.add(eval(node, env)); // eval -> evalT(...).run()
        }
        return out;
    }

    // ---------- QUOTE helper ----------
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

    // ----- COND -----
    private static Trampoline<Object> evaluateCondT(ArrayList<Node<Token>> clauses, Environment env) {
        return Trampoline.more(() -> loopCond(clauses, 0, env));
    }
    private static Trampoline<Object> loopCond(ArrayList<Node<Token>> clauses, int i, Environment env) {
        if (i >= clauses.size())
            throw new RuntimeException("cond: no true clause and no else clause");
        Node<Token> clause = clauses.get(i);
        Node<Token> predicate = clause.getChildren().get(0);
        Node<Token> body      = clause.getChildren().get(1);

        Object result = eval(predicate, env); // trampolined under the hood
        if ("#t".equals(result)) {
            return Trampoline.more(() -> evalT(body, env)); // tail bounce
        }
        return Trampoline.more(() -> loopCond(clauses, i + 1, env));
    }
    // Legacy wrapper
    private static Object evaluateCond(ArrayList<Node<Token>> clauses, Environment env) {
        return evaluateCondT(clauses, env).run();
    }

    // ----- DO -----
    private static Trampoline<Object> evaluateDoT(ArrayList<Node<Token>> todos, Environment env){ 
        return Trampoline.more(() -> loopDo(todos, 0, env));
    }
    private static Trampoline<Object> loopDo(ArrayList<Node<Token>> todos, int i, Environment env) {
        if (todos.isEmpty()) return Trampoline.done(null);
        if (i < todos.size() - 1) {
            eval(todos.get(i), env);                // effect; trampolined
            return Trampoline.more(() -> loopDo(todos, i + 1, env));
        } else {
            return Trampoline.more(() -> evalT(todos.get(i), env)); // final value
        }
    }
    // Legacy wrapper
    private static Object evaluateDo(ArrayList<Node<Token>> todos, Environment env){ 
        return evaluateDoT(todos, env).run();
    }

    // ----- LETS (sequential bindings) -----
    private static Trampoline<Object> evaluateLetsT(ArrayList<Node<Token>> lets, Environment env){
        if (lets.size() != 2) {
            throw new SyntaxException("lets requires a binding list and one body expression");
        }
        Node<Token> bindingsNode = lets.get(0);
        Node<Token> bodyNode = lets.get(1);
        ArrayList<Node<Token>> bindingPairs = bindingsNode.getChildren();

        if (bindingPairs.isEmpty()) {
            return Trampoline.more(() -> evalT(bodyNode, env));
        }

        Node<Token> currentBody = bodyNode;
        for (int i = bindingPairs.size() - 1; i >= 0; i--) {
            Node<Token> binding = bindingPairs.get(i);
            if (binding.getChildren().size() != 2) {
                throw new SyntaxException("Each lets binding must be a (symbol expr) pair");
            }
            Node<Token> labelNode = binding.getChildren().get(0);
            Node<Token> valueExpr = binding.getChildren().get(1);

            Node<Token> lambdaNode = new Node<>(new Token<>("LAMBDA", ""));
            Node<Token> paramsNode = new Node<>(new Token<>("PARAMS", null));
            paramsNode.addChild(labelNode);
            lambdaNode.addChild(paramsNode);
            lambdaNode.addChild(currentBody);
            // IIFE: directly attach arg as a child of the lambda node
            lambdaNode.addChild(valueExpr);

            currentBody = lambdaNode;
        }
        Node<Token> finalBody = currentBody;
        return Trampoline.more(() -> evalT(finalBody, env));
    }
    // Legacy wrapper
    private static Object evaluateLets(ArrayList<Node<Token>> lets, Environment env){
        return evaluateLetsT(lets, env).run();
    }

    // ----- LET (parallel bindings) -----
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

        // Build lambda that takes all bindings as parameters
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

        Object closureTok = eval(lambdaNode, env);
        if (!(closureTok instanceof Token<?,?> closure)) {
            throw new SyntaxException("let expansion did not produce a closure");
        }

        @SuppressWarnings("unchecked")
        Token<String,Object> closureToken = (Token<String,Object>) closure;
        return applyProcedure(closureToken, argVals);
    }

    // --- named let: (let name ((v1 e1) (v2 e2)) body)
    

    private static Trampoline<Object> evaluateLetNamed(ArrayList<Node<Token>> parts, Environment env) {
        if (parts.size() != 3) {
            throw new SyntaxException("named let must have a name, bindings, and body");
        }

        Node<Token> nameNode = parts.get(0);
        Node<Token> bindingsNode = parts.get(1);
        Node<Token> bodyNode = parts.get(2);

        String fnName = (String) nameNode.getValue().value();
        ArrayList<Node<Token>> bindingPairs = bindingsNode.getChildren();

        Node<Token> paramsNode = new Node<>(new Token("PARAMS", null));
        ArrayList<Object> argVals = new ArrayList<>();

        for (Node<Token> binding : bindingPairs) {
            if (binding.getChildren().size() != 2) {
                throw new SyntaxException("Each binding must be a (symbol expr) pair");
            }
            Node<Token> labelNode = binding.getChildren().get(0);
            Node<Token> valueExpr = binding.getChildren().get(1);
            paramsNode.addChild(labelNode);
            argVals.add(evalT(valueExpr, env).run());  // ✅ FIXED
        }

        
        Node<Token> lambdaNode = new Node<>(new Token("LAMBDA", ""));
        lambdaNode.addChild(paramsNode);
        lambdaNode.addChild(bodyNode);

        // Create a *placeholder* closure token for self-reference
        ArrayList<Token> closureParts = new ArrayList<>();
        closureParts.add(new Token<>("VARS", paramsNode.getChildren()));
        closureParts.add(new Token<>("BODY", bodyNode));
        closureParts.add(new Token<>("ENV", env)); // will update to newEnv soon

        @SuppressWarnings("unchecked")
        Token<String,Object> selfClosure = new Token<>("CLOSURE", closureParts);

        // Extend environment with self-reference BEFORE evaluating body
        Environment newEnv = new Environment();
        newEnv.frames.addAll(env.frames);
        newEnv.addFrame(new Pair<>(fnName, selfClosure));

        // Now update captured env to the extended environment (self-aware)
        closureParts.set(2, new Token<>("ENV", newEnv));

        // Apply the self-aware closure
        Object result = applyProcedure(selfClosure, argVals);
        return Trampoline.done(result);

    }


    // ========================================================================
    // Core eval: public wrapper + trampolined engine
    // ========================================================================

    // Public entrypoint: preserve API
    public static Object eval(Node<Token> expr, Environment env){
        return evalT(expr, env).run();
    }

    // Trampolined evaluator
    public static Trampoline<Object> evalT(Node<Token> expr, Environment env){
        Token<?,?> t = expr.getValue();

        // Atoms
        if (isNumber(t) || isBool(t)) {
            return Trampoline.done(t.value());
        }
        if (isString(t)) {
            String str = (String) t.value();
            ArrayList<Object> chars = new ArrayList<>();
            for (int i = 0; i < str.length(); i++) {
                chars.add(String.valueOf(str.charAt(i)));
            }
            return Trampoline.done(new LinkedList<>(chars));
        }

        // (quote …)
        if (isQuote(t)) {
            if (expr.getChildren().size() != 1) {
                throw new SyntaxException("Quote only accepts one argument, received: " + expr.getChildren().size() + "args: "+expr.getChildren());
            }
            Node<Token> quoted = expr.getChildren().get(0);
            if (isList(quoted.getValue())) {
                ArrayList<Object> elems = new ArrayList<>();
                for (Node<Token> child : quoted.getChildren()) {
                    elems.add(quoteToValue(child));
                }
                return Trampoline.done(new LinkedList<>(elems));
            } else {
                return Trampoline.done(quoted.getValue().value());
            }
        }

        // (define name expr)
        if (isDefine(t)){
            String label = (String)expr.getChildren().get(0).getValue().value();
            Object binding = eval(expr.getChildren().get(1), env); // trampolined internally
            env.addFrame(new Pair<>(label, binding));
            return Trampoline.done(env);
        }

        // Special forms (trampolined)
        if (isCond(t)) {
            return evaluateCondT(expr.getChildren(), env);
        }
        if (isDo(t)){ 
            if (expr.getChildren().size() < 1){ 
                throw new SyntaxException("Do blocks require at least one expression"); 
            }
            return evaluateDoT(expr.getChildren(),env);
        }
        if (isLet(t)) {
            // evaluateLet builds/apply a lambda and uses trampolined eval/apply internally
            return Trampoline.done(evaluateLet(expr.getChildren(), env));
        }

        if (isLetNamed(t)) {
            return evaluateLetNamed(expr.getChildren(), env);
        }

        if (isLets(t)) {
            return evaluateLetsT(expr.getChildren(), env);
        }

        // (lambda ...) — build closure, then staged application via applyProcedureT
        if (isLambda(t)) {
            ArrayList<Node<Token>> children = expr.getChildren();
            ArrayList<Token> closureParts = new ArrayList<>();
            @SuppressWarnings("unchecked")
            ArrayList<Node<Token>> params0 = children.get(0).getChildren();
            closureParts.add(new Token<>("VARS", params0));
            Node<Token> body = children.get(1);
            closureParts.add(new Token<>("BODY", body));
            closureParts.add(new Token<>("ENV", env));
            @SuppressWarnings("unchecked")
            Token<String,Object> proc = new Token<>("CLOSURE", closureParts);

            int i = 2;
            while (i < children.size()) {
                Node<Token> ch = children.get(i);
                String cty = (String) ch.getValue().type();

                if ("CALL0".equals(cty)) {
                    final Token<String,Object> currentProc = proc;
                    Object res = applyProcedureT(currentProc, new ArrayList<>()).run();
                    i++;
                    if (res instanceof Token<?,?> rt && isClosure(rt)) {
                        @SuppressWarnings("unchecked")
                        Token<String,Object> next = (Token<String,Object>) rt;
                        proc = next;
                        continue;
                    } else {
                        if (i < children.size()) {
                            throw new SyntaxException("CALL0 applied to non-procedure result with extra arguments remaining");
                        }
                        return Trampoline.done(res);
                    }
                } else {
                    @SuppressWarnings("unchecked")
                    ArrayList<Token> parts = (ArrayList<Token>) proc.value();
                    @SuppressWarnings("unchecked")
                    ArrayList<Node<Token>> paramsNow = (ArrayList<Node<Token>>) parts.get(0).value();
                    int need = paramsNow.size();

                    ArrayList<Object> batch = new ArrayList<>();
                    int taken = 0;
                    while (taken < need && i < children.size()) {
                        if ("CALL0".equals(children.get(i).getValue().type())) break;
                        batch.add(eval(children.get(i), env)); // trampolined under the hood
                        i++; taken++;
                    }
                    Object res = applyProcedureT(proc, batch).run();
                    if (i >= children.size()) return Trampoline.done(res);
                    if (res instanceof Token<?,?> rt && isClosure(rt)) {
                        @SuppressWarnings("unchecked")
                        Token<String,Object> next = (Token<String,Object>) rt;
                        proc = next;
                    } else {
                        throw new IllegalStateException("Too many arguments applied to a non-closure result");
                    }
                }
            }
            return Trampoline.done(proc);
        }

        // Symbol
        if (isSymbol(t)) {
            if (expr.getChildren().isEmpty()) {
                String sym = (String) t.value();
                Object val = env.lookup(sym).orElseThrow(() -> new RuntimeException("Unbound symbol: " + sym));
                return Trampoline.done(val);
            }
            // (symbol arg1 arg2 ...)
            String sym = (String) t.value();
            Object op = env.lookup(sym).orElseThrow(() -> new RuntimeException("Unbound symbol: " + sym));
            ArrayList<Object> argVals = new ArrayList<>();
            for (Node<Token> child : expr.getChildren()) {
                if ("CALL0".equals(child.getValue().type())) continue;
                argVals.add(eval(child, env)); // trampolined
            }
            if (op instanceof Token<?,?> opTok) {
                @SuppressWarnings("unchecked")
                Token<String,Object> procTok = (Token<String,Object>) opTok;
                if (argVals.isEmpty()) {
                    return applyProcedureT(procTok, new ArrayList<>());
                }
                return applyProcedureT(procTok, argVals);
            } else if (op instanceof Supplier<?> supplier) {
                if (!argVals.isEmpty())
                    throw new SyntaxException("Procedure " + sym + " expects 0 arguments, got " + argVals.size());
                return Trampoline.done(supplier.get());
            } else if (op instanceof Function<?,?> fn) {
                if (argVals.size() != 1)
                    throw new SyntaxException("Procedure " + sym + " expects 1 argument, got " + argVals.size());
                @SuppressWarnings("unchecked")
                Function<Object,Object> f = (Function<Object,Object>) fn;
                return Trampoline.done(f.apply(argVals.get(0)));
            } else if (op instanceof BiFunction<?,?,?> bfn) {
                if (argVals.size() != 2)
                    throw new SyntaxException("Procedure " + sym + " expects 2 arguments, got " + argVals.size());
                @SuppressWarnings("unchecked")
                BiFunction<Object,Object,Object> bf = (BiFunction<Object,Object,Object>) bfn;
                return Trampoline.done(bf.apply(argVals.get(0), argVals.get(1)));
            } else {
                throw new SyntaxException("First position is not a procedure: " + sym);
            }
        }

        // Primitive-headed application: (+ 1 2 3), etc.
        if (isPrimitive(t)){
            ArrayList<Object> argVals = new ArrayList<>();
            for (Node<Token> child : expr.getChildren()) {
                argVals.add(eval(child, env)); // trampolined
            }
            @SuppressWarnings("unchecked")
            Token<String,Object> procTok = (Token<String,Object>) t;
            return applyProcedureT(procTok, argVals);
        }

        // (list ...) literal node -> evaluate each element
        if (isList(t)) {
            int dot = -1;
            ArrayList<Node<Token>> kids = expr.getChildren();
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
                return Trampoline.done(cell);
            }
            ArrayList<Object> elems = evaluateList(expr.getChildren(), env);
            return Trampoline.done(new LinkedList<>(elems));
        }

        // General application: evaluate head, then apply if it's a closure
        if (!expr.getChildren().isEmpty()) {
            Object headVal = eval(new Node<>(t), env);
            ArrayList<Object> argVals = new ArrayList<>();
            for (Node<Token> child : expr.getChildren()) {
                argVals.add(eval(child, env));
            }

            if (headVal instanceof Token<?,?> headTok) {
                @SuppressWarnings("unchecked")
                Token<String,Object> procTok = (Token<String,Object>) headTok;
                return applyProcedureT(procTok, argVals);
            }
        }

        throw new SyntaxException("Cannot evaluate expression with head: " + t);
    }

    // ========================================================================
    // Binding helpers + procedure application
    // ========================================================================

    private static void bind (ArrayList<Node<Token>> vars, ArrayList<Object> args, Environment env){
        if (vars.size() == args.size()) {
            List<Pair<String, Object>> bindings = new ArrayList<>();
            for (int i = 0; i < vars.size(); i++) {
                Node<Token> var = vars.get(i);
                Object arg = args.get(i);
                Token<?,?> varTok = var.getValue();
                String name = (String) varTok.value();
                bindings.add(new Pair<>(name, arg));
            }
            env.addFrame(bindings);
        } else  {
            throw new IllegalStateException("Variable count mismatch");
        }
    }

    // Legacy bridge for older sites (e.g., evaluateLet)
    private static Object applyProcedure(Token<String,Object> proc, ArrayList<Object> args) {
        return applyProcedureT(proc, args).run();
    }

    // Trampolined procedure application: lambda body is executed via bounce
    public static Trampoline<Object> applyProcedureT(Token<String,Object> proc, ArrayList<Object> args) {
        if (isPrimitive(proc)) {
            String opName = (String) proc.value();
            return Trampoline.done(applyPrimitive(opName, args));
        } else if (isClosure(proc)) {
            @SuppressWarnings("unchecked")
            ArrayList<Token> closureParts = (ArrayList<Token>) proc.value();

            @SuppressWarnings("unchecked")
            ArrayList<Node<Token>> params = (ArrayList<Node<Token>>) closureParts.get(0).value();
            Node<Token> body = (Node<Token>) closureParts.get(1).value();
            Environment capturedEnv = (Environment) closureParts.get(2).value();

            Environment newEnv = new Environment();
            newEnv.frames.addAll(capturedEnv.frames);

            ArrayList<Object> normalizedArgs = new ArrayList<>();
            for (Object a : args) {
                if (a instanceof Node) {
                    @SuppressWarnings("unchecked")
                    Node<Token> n = (Node<Token>) a;
                    normalizedArgs.add(eval(n, newEnv)); // trampolined internally
                } else {
                    normalizedArgs.add(a);
                }
            }
            // Allow zero-arg call or exact match
            if (params.size() != normalizedArgs.size()) {
                if (!(params.isEmpty() && normalizedArgs.isEmpty())) {
                    throw new IllegalStateException(
                        "Variable count mismatch: expected " + params.size() + " but got " + normalizedArgs.size());
                }
            }
            if (!params.isEmpty()) {
                bind(params, normalizedArgs, newEnv);
            }

            // Tail position bounce: evaluate the body via trampoline
            return Trampoline.more(() -> evalT(body, newEnv));
        } else {
            throw new SyntaxException("First position is not a procedure: " + proc);
        }
    }

    private static Object applyPrimitive(String opName, ArrayList<Object> args) {
        Object primitive = getPrimitive(opName);
        // ---- BiFunction primitives (variadic support) ----
        if (primitive instanceof BiFunction<?, ?, ?> bi) {
            @SuppressWarnings("unchecked")
            BiFunction<Object, Object, Object> op = (BiFunction<Object, Object, Object>) bi;
            if (args.isEmpty()) {
                if (opName.equals("PLUS")) return BigInteger.ZERO;
                if (opName.equals("MULTIPLY")) return BigInteger.ONE;
                throw new IllegalStateException(opName + " requires at least one argument");
            }
            Object acc = args.get(0);
            for (int i = 1; i < args.size(); i++) {
                acc = op.apply(acc, args.get(i));
            }
            return acc;
        }
        // ---- Function primitives (single-arg) ----
        else if (primitive instanceof Function<?, ?> fn) {
            if (args.size() != 1)
                throw new IllegalStateException(opName + " expects 1 argument, got " + args.size());
            @SuppressWarnings("unchecked")
            Function<Object, Object> op = (Function<Object, Object>) fn;
            return op.apply(args.get(0));
        }
        // ---- Supplier primitives (zero-arg) ----
        else if (primitive instanceof Supplier<?> supplier) {
            if (!args.isEmpty())
                throw new IllegalStateException(opName + " expects 0 arguments, got " + args.size());
            return supplier.get();
        }
        throw new IllegalStateException("Unknown primitive type for operator: " + opName);
    }
}
