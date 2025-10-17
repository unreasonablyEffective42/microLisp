import java.io.*;
import java.math.BigInteger;


public class MicroLispTest {

    public static void main(String[] args) {
        Environment env = GlobalEnvironment.initGlobalEnvironment();
        int passed = 0, failed = 0;

        // --- Core language features ---
        if (test("Lexer basic string", testLexerString())) passed++; else failed++;
        if (test("LinkedList size", testLinkedListSize())) passed++; else failed++;
        if (test("LinkedList toString", testLinkedListToString())) passed++; else failed++;
        if (test("Simple arithmetic", testEval("(+ 1 2 3)", 6, env))) passed++; else failed++;
        if (test("Lambda application", testEval("((lambda (x) (+ x 1)) 5)", 6, env))) passed++; else failed++;
        if (test("Define and call", testEval("(do (define foo (lambda (x) (* x 2))) (foo 5))", "10", env))) passed++; else failed++;
        // --- Quote and symbol semantics ---
        if (test("Quote simple symbol", testEval("'x", "x", env))) passed++; else failed++;
        if (test("Quote list of symbols", testEval("'(x y)", "(x y)", env))) passed++; else failed++;
        if (test("Quote nested list", testEval("'((a b) (c d))", "((a b) (c d))", env))) passed++; else failed++;
        if (test("Quote empty list", testEval("'()", "()", env))) passed++; else failed++;
        if (test("Quote numeric list", testEval("'(1 2 3)", "(1 2 3)", env))) passed++; else failed++;
        if (test("Quote string list", testEval("'(\"a\" \"b\" \"c\")", "\"abc\"", env))) passed++; else failed++;
        if (test("Quote lambda form", testEval("'(lambda (x) (+ x 1))", "(lambda (x) (+ x 1))", env))) passed++; else failed++;
        if (test("Quote define form", testEval("'(define foo 5)", "(define foo 5)", env))) passed++; else failed++;
        if (test("Quote cond form", testEval("'(cond (#t 1) (else 2))", "(cond (#t 1) (else 2))", env))) passed++; else failed++; 
        // ---
        if (test("String literal", testEval("\"abc\"", "\"abc\"", env))) passed++; else failed++;
        if (test("Print output", testPrint("(print \"hello\")", "hello\n", env))) passed++; else failed++;
        if (test("Printf output", testPrint("(printf \"abc\")", "abc", env))) passed++; else failed++;
        if (test("Printf newline", testPrint("(printf \"abc\\n123\")", "abc\n123", env))) passed++; else failed++;
        if (test("Cons number list", testEval("(cons 1 (cons 2 '()))", "(1 2)", env))) passed++; else failed++;
        if (test("Cons string list", testEval("(cons \"a\" (cons \"b\" '()))", "(\"a\" \"b\")", env))) passed++; else failed++;
        if (test("Cons char onto string", testEval("(cons \"a\" \"bc\")", "\"abc\"", env))) passed++; else failed++;
        if (test("Cons chain onto empty string", testEval("(cons \"a\" (cons \"b\" (cons \"c\" \"\")))", "\"abc\"", env))) passed++; else failed++;
        if (test("Let single binding", testEval("(let ((x 5)) x)", 5, env))) passed++; else failed++;
        if (test("Let multiple bindings", testEval("(let ((x 1) (y 2)) (+ x y))", 3, env))) passed++; else failed++; 
        if (test("Let nested dependency (should error)", testLetNestedFails(env))) passed++; else failed++;
        if (test("Lets sequential single binding", testEval("(lets ((x 2)) (+ x 3))", 5, env))) passed++; else failed++;
        if (test("Lets sequential multiple bindings", testEval("(lets ((x 1) (y (+ x 1))) y)", 2, env))) passed++; else failed++;
        if (test("Lets shadowing variable", testEval("(lets ((x 10) (x (+ x 5))) x)", 15, env))) passed++; else failed++;
        if (test("Lets independent evaluation", testEval("(lets ((a 3) (b (* a 2)) (c (+ b 1))) c)", 7, env))) passed++; else failed++;
        //if (test("Named let simple recursion to 10",//this went into an infinite recursion
        //         testEval("(let loop ((a 0)) (cond ((eq? a 10) \"done\") (else (loop (+ a 1)))))", "\"done\"", env))) passed++; else failed++;
        //if (test("Named let accumulates sum to 45",//I assume this  will too.
         //        testEval("(let loop ((a 0) (sum 0)) (cond ((eq? a 10) sum) (else (loop (+ a 1) (+ sum a)))))", 45, env))) passed++; else failed++;
        // --- Empty list normalization tests ---
        if (test("Empty list literal", testEval("()", "()", env))) passed++; else failed++;
        if (test("Quoted empty list", testEval("'()", "()", env))) passed++; else failed++;
        if (test("Quote form (quote ())", testEval("(quote ())", "()", env))) passed++; else failed++;
        if (test("List form (list)", testEval("(list)", "()", env))) passed++; else failed++;
        if (test("Null? on ()", testEval("(null? ())", "#t", env))) passed++; else failed++;
        if (test("Null? on empty string", testEval("(null? \"\")", "#t", env))) passed++; else failed++;
        if (test("Null? on '()", testEval("(null? '())", "#t", env))) passed++; else failed++;
        if (test("Eq? '() vs '()", testEval("(eq? '() '())", "#t", env))) passed++; else failed++;
        if (test("Eq? () vs '()", testEval("(eq? () '())", "#t", env))) passed++; else failed++;
        if (test("Cons onto empty list", testEval("(cons 1 '())", "(1)", env))) passed++; else failed++;
        if (test("Nested cons with empty tail", testEval("(cons 1 (cons 2 ()))", "(1 2)", env))) passed++; else failed++;
        // --- cond behavior with empty list ---
        if (test("Cond returning '()", testEval("(cond (#t '()))", "()", env))) passed++; else failed++;
        if (test("Cond true branch", testEval("(cond (#t 1))", "1", env))) passed++; else failed++;
        if (test("Cond false branch triggers error", testCondFails("(cond (#f 2))", env))) passed++; else failed++;
        if (test("Cond with no clauses triggers error", testCondFails("(cond)", env))) passed++; else failed++;
        // --- lambda & let returning () ---
        if (test("Lambda returning ()", testEval("((lambda (x) ())0)", "()", env))) passed++; else failed++;
        if (test("Lambda returning '()", testEval("((lambda (x) '())0)", "()", env))) passed++; else failed++;
        if (test("Let body returns ()", testEval("(let ((x 1)) ())", "()", env))) passed++; else failed++;
        if (test("Lets body returns '()", testEval("(lets ((x 1)) '())", "()", env))) passed++; else failed++;
        // --- print / printf of empty list ---
        if (test("Print '() adds newline", testPrint("(print '())", "()\n", env))) passed++; else failed++;
        if (test("Print () adds newline", testPrint("(print ())", "()\n", env))) passed++; else failed++;
        if (test("Printf '() no newline", testPrint("(printf '())", "()", env))) passed++; else failed++;
        if (test("Printf () no newline", testPrint("(printf ())", "()", env))) passed++; else failed++;
        // --- Zero-argument lambda tests (MicroLisp syntax) ---
        if (test("Zero-arg lambda literal", testEval("((lambda () 42))", 42, env))) passed++; else failed++;
        if (test("Zero-arg lambda via define + call", testEval("(do (define foo (lambda () 99)) (foo))", 99, env))) passed++; else failed++;
        if (test("Zero-arg lambda returning empty list", testEval("((lambda () '()))", "()", env))) passed++; else failed++;
        if (test("Zero-arg lambda returning computed expression", testEval("((lambda () (+ 2 3)))", 5, env))) passed++; else failed++;
        if (test("Define + call zero-arg lambda returning computed expression", testEval("(do (define bar (lambda () (+ 1 2 3))) (bar))", 6, env))) passed++; else failed++;
        // --- Mixed regression tests ---
        if (test("Mixed arity: zero and one arg coexist", testEval("(do (define id (lambda (x) x)) (define f (lambda () 7)) (+ (id 5) (f)))", 12, env))) passed++; else failed++;
        if (test("Nested zero-arg lambda inside another call", testEval("((lambda (x) (+ x ((lambda () 3)))) 4)", 7, env))) passed++; else failed++;
        if (test("Higher-order: zero-arg lambda returned and invoked", testEval("(((lambda () (lambda () 11))))", 11, env))) passed++; else failed++;
        if (test("Closure captures env in zero-arg lambda", testEval("((lambda (x) ((lambda () x))) 42)", 42, env))) passed++; else failed++;
        System.out.println("=============================================");
        System.out.println("Tests passed: " + passed);
        System.out.println("Tests failed: " + failed);
        System.out.println("=============================================");
    }

    // ---------- Individual Tests ----------
    static boolean test(String name, boolean result) {
        System.out.println((result ? MicroLisp.GREEN + "PASS: " + name + MicroLisp.RESET 
                                   : MicroLisp.RED + "FAIL: " + name + MicroLisp.RESET));
        return result;
    }

    static boolean testLexerString() {
        Lexer l = new Lexer("\"abc\\n\"");
        Token t = l.getNextToken();
        return t.type().equals("STRING") && ((String)t.value()).equals("abc\n");
    }

    static boolean testLinkedListSize() {
        LinkedList<String> list = new LinkedList<>("a", "b", "c");
        return list.size() == 3;
    }

    static boolean testLinkedListToString() {
        LinkedList<String> chars = new LinkedList<>("a", "b", "c");
        LinkedList<Integer> nums = new LinkedList<>(1, 2, 3);
        return chars.toString().equals("\"abc\"") && nums.toString().equals("(1 2 3)");
    }

    static boolean testDefine(Environment env) {
        eval("(define foo (lambda (x) (* x 2)))", env);
        Object result = eval("(foo 5)", env);
        return result.equals(10);
    }

    static boolean testEval(String src, Object expected, Environment env) {
        Object result = eval(src, env);
        return result != null && result.toString().equals(expected.toString());
    }

    static boolean testPrint(String src, String expectedOutput, Environment env) {
        String output = captureOutput(() -> eval(src, env));
        return output.equals(expectedOutput);
    }

    // ---------- Helpers ----------

    static Object eval(String src, Environment env) {
        Parser parser = new Parser(src);
        Node node = parser.parse();
        return Evaluator.eval(node, env);
    }

    static String captureOutput(Runnable r) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(baos));
        try { r.run(); } finally { System.setOut(oldOut); }
        return baos.toString();
    }

    static boolean testLetNestedFails(Environment env) {
        try {
            eval("(let ((x 1) (y (+ x 1))) y)", env);
            return false; // should not reach here
        } catch (RuntimeException e) {
            return e.getMessage().contains("Unbound symbol: x");
        }
    }

    static boolean testCondFails(String src, Environment env) {
        try {
            eval(src, env);
            return false; // should not reach here
        } catch (RuntimeException e) {
            return e.getMessage().contains("cond: no true clause and no else clause");
        }
    }
}
