import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.Consumer;
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
    public static boolean isType      (Token<?, ?> t, String ty) { return ty.equals(t.type()); }
    public static boolean isNumber    (Token<?, ?> t){ return isType(t, "NUMBER"); }
    public static boolean isString    (Token<?, ?> t){ return isType(t, "STRING"); }
    public static boolean isSymbol    (Token<?, ?> t){ return isType(t, "SYMBOL"); }
    public static boolean isLambda    (Token<?, ?> t){ return isType(t, "LAMBDA"); }
    public static boolean isList      (Token<?, ?> t){ return isType(t, "LIST"); }
    public static boolean isCond      (Token<?, ?> t){ return isType(t, "COND"); }
    public static boolean isQuote     (Token<?, ?> t){ return isType(t, "QUOTE") || isType(t, "SYMBOL") && "quote".equals(t.value()); }
    public static boolean isBool      (Token<?, ?> t){ return isType(t, "BOOLEAN"); }
    public static boolean isClosure   (Token<?, ?> t){ return isType(t, "CLOSURE"); }
    public static boolean isDefine    (Token<?, ?> t){ return isType(t, "DEFINE"); } 
    public static boolean isDo        (Token<?, ?> t){ return isType(t, "DO"); }
    public static boolean isLet       (Token<?, ?> t){ return isType(t, "LET"); }
    public static boolean isLets      (Token<?, ?> t){ return isType(t, "LETS"); }
    public static boolean isLetr      (Token<?, ?> t){ return isType(t, "LETR"); }
    public static boolean isLetNamed  (Token<?, ?> t){ return isType(t, "LET-NAMED"); }
    public static boolean isApply     (Token<?, ?> t){ return isType(t, "APPLY"); }
    public static boolean isAtom      (Token<?, ?> t){ return isType(t, "NUMBER") || isType(t, "BOOLEAN") ; }

    // ---------- list evaluation -------- 
    private static ArrayList<Object> evaluateList(ArrayList<Node<Token>> list, Environment env){
        ArrayList<Object> out = new ArrayList<>(list.size());
        for (Node<Token> node : list) {
            out.add(eval(node, env)); // eval -> evalT(...).run()
        }
        return out;
    }

private static Object quoteToValue(Node<Token> node) {
    Token<?,?> tok = node.getValue();

    // Empty list
    if ((tok == null || tok.type() == null) && node.getChildren().isEmpty()) {
        return new LinkedList<>();
    }

    // Quoted list
    if (tok != null && "LIST".equals(tok.type())) {
        ArrayList<Node<Token>> children = node.getChildren();
        if (children.isEmpty()) {
            return new LinkedList<>();
        }

        boolean allSingleChars = true;
        StringBuilder chars = new StringBuilder(children.size());
        for (Node<Token> child : children) {
            Token<?,?> childTok = child.getValue();
            if (childTok == null || !"STRING".equals(childTok.type())) {
                allSingleChars = false;
                break;
            }
            String val = (String) childTok.value();
            if (val.length() != 1) {
                allSingleChars = false;
                break;
            }
            chars.append(val);
        }
        if (allSingleChars) {
            return LinkedList.fromString(chars.toString());
        }

        ArrayList<Object> elems = new ArrayList<>();
        for (Node<Token> child : children) {
            elems.add(quoteToValue(child));
        }
        return new LinkedList<>(elems);
    }

    // Nested quote nodes → build (quote <datum>) recursively
    if (tok != null && "QUOTE".equals(tok.type())) {
        ArrayList<Object> elems = new ArrayList<>();
        elems.add(new Symbol("quote"));
        if (!node.getChildren().isEmpty()) {
            elems.add(quoteToValue(node.getChildren().get(0)));
        }
        return new LinkedList<>(elems);
    }

    // Strings become linked-list char sequences
    if (tok != null && "STRING".equals(tok.type())) {
        return LinkedList.fromString((String) tok.value());
    }

    // Primitives and symbols stay as symbols/numbers
    if (tok != null) {
        String ttype = String.valueOf(tok.type());
        Object tval  = tok.value();
        switch (ttype) {
            case "LAMBDA":    return new Symbol("lambda");
            case "COND":      return new Symbol("cond");
            case "DO":        return new Symbol("do");
            case "LET":       return new Symbol("let");
            case "LETS":      return new Symbol("lets");
            case "LETR":      return new Symbol("letr");
            case "DEFINE":    return new Symbol("define");
            case "SYMBOL":    return new Symbol((String) tval);
            case "BOOLEAN":   return new Symbol((String) tval);
            case "NUMBER":    return tval;
        }
    }

    // Default
    return tok == null ? null : tok.value();
}
    // ----- COND -----
    private static Trampoline<Object> evaluateCondT(ArrayList<Node<Token>> clauses, Environment env) {
        return Trampoline.more(() -> loopCond(clauses, 0, env));
    }

    private static Trampoline<Object> loopCond(ArrayList<Node<Token>> clauses, int i, Environment env) {
        if (i >= clauses.size())
            throw new RuntimeException("cond: no true clause and no else clause");

        Node<Token> clause = clauses.get(i);
        ArrayList<Node<Token>> kids = clause.getChildren();
        if (kids.isEmpty())
            return Trampoline.more(() -> loopCond(clauses, i + 1, env));

        Node<Token> predNode = kids.get(0);
        ArrayList<Node<Token>> bodies = new ArrayList<>(kids.subList(1, kids.size()));
        // Handle (else ...)
        Token<?, ?> tok = predNode.getValue();
        boolean isElse = "SYMBOL".equals(tok.type()) && "else".equals(tok.value());
        if (isElse) {
            if (bodies.isEmpty())
                throw new SyntaxException("cond else clause requires at least one body expression");
            return bodies.size() == 1
                    ? Trampoline.more(() -> evalT(bodies.get(0), env))
                    : evaluateDoT(bodies, env);
        }
        // Evaluate predicate
        Object predValue = eval(predNode, env);
        if ("#t".equals(predValue)) {
            if (bodies.isEmpty())
                return Trampoline.done(predValue);
            return bodies.size() == 1
                    ? Trampoline.more(() -> evalT(bodies.get(0), env))
                    : evaluateDoT(bodies, env);
        }
        // Try next clause
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
            argVals.add(evalT(valueExpr, env).run());
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
            return Trampoline.done(LinkedList.fromString(str));
        }
 
        // (quote …)
        if (isQuote(t)) {
            if (expr.getChildren().size() != 1) {
                throw new SyntaxException("quote takes exactly one argument, got: " + expr.getChildren().size());
            }
            // Convert the quoted *datum* (which may itself be a QUOTE node, a LIST, or an atom)
            // into a runtime value using quoteToValue for all cases.
            Node<Token> quoted = expr.getChildren().get(0);
            return Trampoline.done(quoteToValue(quoted));
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

        // (:: 1 2 3) — tuple literal constructor
        if ("::".equals(t.value())) {
            ArrayList<Object> elems = new ArrayList<>();
            for (Node<Token> child : expr.getChildren()) {
                elems.add(eval(child, env)); // evaluate each element
            }
            // Create tuple based on arity (dispatches to Tuple2..Tuple9 or varargs fallback)
            return Trampoline.done(Tuple.of(elems.toArray()));
        }
        // ($ 1 2 3) - vector literal constructor 
        if ("$".equals(t.value())) {
            ArrayList<Object> elems = new ArrayList<>();
            for (Node<Token> child : expr.getChildren()) {
               elems.add(eval(child, env));
            }
            return Trampoline.done(Vector.of(elems.toArray()));
        }

        // (import foo) special form — do NOT evaluate the argument
        if ("import".equals(t.value())) {
            if (expr.getChildren().size() != 1) {
                throw new SyntaxException("import takes exactly one argument");
            }
            Node<Token> argNode = expr.getChildren().get(0);

            Object rawArg;
            Token<?,?> argTok = argNode.getValue();
            // Extract symbol name or string literal directly
            if (isSymbol(argTok)) {
                rawArg = new Symbol((String) argTok.value());
            } else if (isString(argTok)) {
                rawArg = argTok.value();
            } else {
                throw new SyntaxException("import: expected symbol or string, got " + argTok.type());
            }

            Object importer = env.lookup("import")
                .orElseThrow(() -> new RuntimeException("import function not bound"));

            if (importer instanceof Function<?,?> f) {
                @SuppressWarnings("unchecked")
                Function<Object,Object> f1 = (Function<Object,Object>) f;
                return Trampoline.done(f1.apply(rawArg));
            } else {
                throw new SyntaxException("import binding is not callable: " + importer);
            }
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
            } else if (op instanceof Function<?,?>) {
                // Could be unary Function<Object,Object> or variadic Function<LinkedList<?>,Object>
                // We'll try unary first, and fall back to variadic if the arity doesn't match.
                @SuppressWarnings("unchecked")
                Function<Object,Object> f1 = (Function<Object,Object>) op;

                if (argVals.size() == 1) {
                    // standard single-argument function
                    return Trampoline.done(f1.apply(argVals.get(0)));
                } else {
                    // assume it's a variadic function that expects the entire argument list
                    @SuppressWarnings("unchecked")
                    Function<LinkedList<?>,Object> fvar = (Function<LinkedList<?>,Object>) op;
                    LinkedList<Object> listArgs = new LinkedList<>(argVals);
                    return Trampoline.done(fvar.apply(listArgs));
                }

            } else if (op instanceof BiFunction<?,?,?>) {
                if (argVals.size() != 2)
                    throw new SyntaxException("Procedure " + sym + " expects 2 arguments, got " + argVals.size());
                @SuppressWarnings("unchecked")
                BiFunction<Object,Object,Object> bf = (BiFunction<Object,Object,Object>) op;
                return Trampoline.done(bf.apply(argVals.get(0), argVals.get(1)));

            } else if (op instanceof Supplier<?>) {
                if (!argVals.isEmpty())
                    throw new SyntaxException("Procedure " + sym + " expects 0 arguments, got " + argVals.size());
                @SuppressWarnings("unchecked")
                Supplier<Object> sup = (Supplier<Object>) op;
                return Trampoline.done(sup.get());

            } else if (op instanceof Consumer<?>) {
                if (argVals.size() != 1)
                    throw new SyntaxException("Procedure " + sym + " expects 1 argument, got " + argVals.size());
                @SuppressWarnings("unchecked")
                Consumer<Object> con = (Consumer<Object>) op;
                con.accept(argVals.get(0));
                return Trampoline.done("#t");

            } else if (op instanceof TriFunction<?,?,?,?>) {
                if (argVals.size() != 3)
                    throw new SyntaxException("Procedure " + sym + " expects 3 arguments, got " + argVals.size());
                @SuppressWarnings("unchecked")
                TriFunction<Object,Object,Object,Object> tf = (TriFunction<Object,Object,Object,Object>) op;
                return Trampoline.done(tf.apply(argVals.get(0), argVals.get(1), argVals.get(2)));

            } else if (op instanceof QuadFunction<?,?,?,?,?>) {
                if (argVals.size() != 4)
                    throw new SyntaxException("Procedure " + sym + " expects 4 arguments, got " + argVals.size());
                @SuppressWarnings("unchecked")
                QuadFunction<Object,Object,Object,Object,Object> tf = (QuadFunction<Object,Object,Object,Object,Object>) op;
                return Trampoline.done(tf.apply(argVals.get(0), argVals.get(1), argVals.get(2), argVals.get(3)));

            } else if (op instanceof PentaFunction<?,?,?,?,?,?>) {
                if (argVals.size() != 5)
                    throw new SyntaxException("Procedure " + sym + " expects 5 arguments, got " + argVals.size());
                @SuppressWarnings("unchecked")
                PentaFunction<Object,Object,Object,Object,Object,Object> tf =
                    (PentaFunction<Object,Object,Object,Object,Object,Object>) op;
                return Trampoline.done(
                    tf.apply(argVals.get(0), argVals.get(1), argVals.get(2), argVals.get(3), argVals.get(4)));

            } else if (op instanceof BiConsumer<?,?>) {
                if (argVals.size() != 2)
                    throw new SyntaxException("Procedure " + sym + " expects 2 arguments, got " + argVals.size());
                @SuppressWarnings("unchecked")
                BiConsumer<Object,Object> bc = (BiConsumer<Object,Object>) op;
                bc.accept(argVals.get(0), argVals.get(1));
                return Trampoline.done("#t");

            } else if (op instanceof TriConsumer<?,?,?>) {
                if (argVals.size() != 3)
                    throw new SyntaxException("Procedure " + sym + " expects 3 arguments, got " + argVals.size());
                @SuppressWarnings("unchecked")
                TriConsumer<Object,Object,Object> tc = (TriConsumer<Object,Object,Object>) op;
                tc.accept(argVals.get(0), argVals.get(1), argVals.get(2));
                return Trampoline.done("#t");
            }

            // --- tuple as callable object ---
            else if (op instanceof Tuple tup) {
                if (argVals.size() != 1)
                    throw new SyntaxException("Tuple call expects exactly 1 index argument");
                Object idxObj = argVals.get(0);
                if (!(idxObj instanceof Number))
                    throw new SyntaxException("Tuple index must be a number, got: " + idxObj);

                int index =(int)((Number) idxObj).intVal;
                if (index < 0 || index > tup.size())
                    throw new IndexOutOfBoundsException("Tuple index " + index + " out of range [1," + tup.size() + "]");

                return Trampoline.done(tup.get(index));
            } 
            // --- vector as callable object ---
            else if (op instanceof Vector vec) {
                if (argVals.size() != 1)
                    throw new SyntaxException("Vector call expects exactly 1 index argument");
                Object idxObj = argVals.get(0);
                if (!(idxObj instanceof Number))
                    throw new SyntaxException("Vector index must be a number, got: " + idxObj);
                int index = (int)((Number) idxObj).intVal;
                if (index < 0 || index > vec.size)
                    throw new IndexOutOfBoundsException("Vector index " + index + "out of range [1," + vec.size + "]");
                return Trampoline.done(vec.elems[index]);
            }

            else {
                throw new SyntaxException("First position is not a procedure: " + sym);
            }
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

        // (apply op-expr arg1 arg2 ...)
        if (isApply(t)) {
            if (expr.getChildren().isEmpty()) {
                throw new SyntaxException("APPLY requires an operator expression");
            }

            Node<Token> opNode = expr.getChildren().get(0);
            Object opVal = eval(opNode, env);

            ArrayList<Object> argVals = new ArrayList<>();
            for (int i = 1; i < expr.getChildren().size(); i++) {
                argVals.add(eval(expr.getChildren().get(i), env));
            }

            if (opVal instanceof Token<?, ?> tok) {
                @SuppressWarnings("unchecked")
                Token<String, Object> procTok = (Token<String, Object>) tok;
                return applyProcedureT(procTok, argVals);
            }

            if (opVal instanceof Supplier<?> s) {
                if (!argVals.isEmpty())
                    throw new SyntaxException("Procedure expects 0 arguments, got " + argVals.size());
                return Trampoline.done(s.get());
            } else if (opVal instanceof Function<?, ?> f) {
                @SuppressWarnings("unchecked")
                Function<Object, Object> f1 = (Function<Object, Object>) f;
                if (argVals.size() == 1) return Trampoline.done(f1.apply(argVals.get(0)));
                @SuppressWarnings("unchecked")
                Function<LinkedList<?>, Object> fvar = (Function<LinkedList<?>, Object>) f;
                return Trampoline.done(fvar.apply(new LinkedList<>(argVals)));
            } else if (opVal instanceof BiFunction<?, ?, ?> bf) {
                if (argVals.size() != 2)
                    throw new SyntaxException("Procedure expects 2 arguments, got " + argVals.size());
                @SuppressWarnings("unchecked")
                BiFunction<Object, Object, Object> op = (BiFunction<Object, Object, Object>) bf;
                return Trampoline.done(op.apply(argVals.get(0), argVals.get(1)));
            } else if (opVal instanceof Vector v) {
                return Trampoline.done(applyIndexing(v, argVals));
            }

            throw new SyntaxException("First position is not a procedure: " + opVal);
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
        if (isClosure(proc)) {
            @SuppressWarnings("unchecked")
            ArrayList<Token> closureParts = (ArrayList<Token>) proc.value();

            @SuppressWarnings("unchecked")
            ArrayList<Node<Token>> params = (ArrayList<Node<Token>>) closureParts.get(0).value();
            Node<Token> body = (Node<Token>) closureParts.get(1).value();
            Environment capturedEnv = (Environment) closureParts.get(2).value();            
            Environment newEnv = new Environment();
            for (Frame f : capturedEnv.frames) {
                List<Pair<String,Object>> copied = new ArrayList<>();
                for (Pair<String,Object> p : f.bindings) {
                    copied.add(new Pair<>(p.first, p.second));
                }
                newEnv.frames.add(new Frame(copied));
            }
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
        }
               else {
            throw new SyntaxException("First position is not a procedure: " + proc);
        }
    }

    private static Object applyIndexing(Object target, List<Object> idxs) {
        if (idxs.isEmpty()) {
            throw new SyntaxException("Indexing expects at least one index");
        }
        Object current = target;
        int from = 0;
        while (from < idxs.size()) {
            int i = toIndex(idxs.get(from++));  // 0-based per your example
            if (current instanceof Vector v) {
                boundsCheck(i, v.size);
                current = v.elems[i];
            }
/*
            else if (current instanceof Tuple t) {
                boundsCheck(i, t.size());
                current = t.get(i);
            }
            else if (current instanceof LinkedList<?> l) {
                current = listNth(l, i); // implement efficient nth or iterate
            } 
*/
            else {
                throw new SyntaxException("Cannot index into: " + current);
            }
        }
        return current;
    }

    private static int toIndex(Object o) {
        // Your tower likely has its own Number type; support both.
        if (o instanceof Number n) { // your numeric tower
            return (int) n.intVal;     // or n.toInt(), whatever your API is
        } else if (o instanceof java.lang.Number jn) {
            return jn.intValue();
        }
        throw new SyntaxException("Index must be numeric, got: " + o);
    }

    private static void boundsCheck(int i, int n) {
        if (i < 0 || i >= n) throw new SyntaxException("Index out of bounds: " + i + " (size " + n + ")");
    }

    // Example nth for your Lisp list if you want to support it:
    private static Object listNth(LinkedList<?> l, int i) {
        for (int k = 0; k < i; k++) {
            if (l.tail() == null) throw new SyntaxException("Index out of bounds: " + i);
            l = (LinkedList<?>) l.tail();
        }
        return l.head();
    }
}
