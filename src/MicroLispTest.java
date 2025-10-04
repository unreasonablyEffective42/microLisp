import java.io.*;

public class MicroLispTest {

    public static void main(String[] args) {
        Environment env = MicroLisp.makeGlobalEnv();
        int passed = 0, failed = 0;

        // Run all tests
        if (test("Lexer basic string", testLexerString())) passed++; else failed++;
        if (test("LinkedList size", testLinkedListSize())) passed++; else failed++;
        if (test("LinkedList toString", testLinkedListToString())) passed++; else failed++;
        if (test("Simple arithmetic", testEval("(+ 1 2 3)", 6, env))) passed++; else failed++;
        if (test("Lambda application", testEval("((lambda (x) (+ x 1)) 5)", 6, env))) passed++; else failed++;
        if (test("Define and call", testDefine(env))) passed++; else failed++;
        if (test("Quote list", testEval("'(1 2 3)", "(1 2 3)", env))) passed++; else failed++;
        if (test("String literal", testEval("\"abc\"", "\"abc\"", env))) passed++; else failed++;
        if (test("Print output", testPrint("(print \"hello\")", "hello\n", env))) passed++; else failed++;
        if (test("Printf output", testPrint("(printf \"abc\")", "abc", env))) passed++; else failed++;
        if (test("Printf newline", testPrint("(printf \"abc\\n123\")", "abc\n123", env))) passed++; else failed++;
        if (test("Cons number list", testEval("(cons 1 (cons 2 '()))", "(1 2)", env))) passed++; else failed++;
        if (test("Cons string list", testEval("(cons \"a\" (cons \"b\" '()))", "(\"a\" \"b\")", env))) passed++; else failed++;
        if (test("Let single binding", testEval("(let ((x 5)) x)", 5, env))) passed++; else failed++;
        if (test("Let multiple bindings", testEval("(let ((x 1) (y 2)) (+ x y))", 3, env))) passed++; else failed++; 
        if (test("Let nested dependency (should error)", testLetNestedFails(env))) passed++; else failed++;
        if (test("Lets sequential single binding", testEval("(lets ((x 2)) (+ x 3))", 5, env))) passed++; else failed++;
        if (test("Lets sequential multiple bindings", testEval("(lets ((x 1) (y (+ x 1))) y)", 2, env))) passed++; else failed++;
        if (test("Lets shadowing variable", testEval("(lets ((x 10) (x (+ x 5))) x)", 15, env))) passed++; else failed++;
        if (test("Lets independent evaluation", testEval("(lets ((a 3) (b (* a 2)) (c (+ b 1))) c)", 7, env))) passed++; else failed++;
        System.out.println("============================================="); 
        System.out.println("Tests passed: " + passed);
        System.out.println("Tests failed: " + failed);
        System.out.println("=============================================");
    }

    // ---------- Individual Tests ----------

    static boolean test(String name, boolean result) {
        System.out.println((result ? MicroLisp.GREEN + "PASS: " + name + MicroLisp.RESET : MicroLisp.RED + "FAIL: " + name + MicroLisp.RESET));
        return result;
    }

    static boolean testLexerString() {
        Lexer l = new Lexer("\"abc\\n\"");
        Token t = l.getNextToken();
        boolean ok = t.type().equals("STRING") && ((String)t.value()).equals("abc\n");
        return ok;
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
}
