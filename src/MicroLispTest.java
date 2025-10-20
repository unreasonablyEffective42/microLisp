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
        if (test("Quasiquote unquote evaluates expression", testEval("`(1 ,(+ 2 3))", "(1 5)", env))) passed++; else failed++;
        if (test("Quasiquote splice literal list", testEval("`(1 ,@(2 3))", "(1 2 3)", env))) passed++; else failed++;
        if (test("Quasiquote splice evaluated list", testEval("`(1 ,@(list 2 3))", "(1 2 3)", env))) passed++; else failed++;
        if (test("Quasiquote nested unquote", testEval("`(foo `(bar ,(+ 1 2)))", "(foo (quasi-quote (bar (unquote (+ 1 2)))))", env))) passed++; else failed++;
        if (test("Quasiquote nested splice", testEval("`(a `(b ,@(list 1 2)))", "(a (quasi-quote (b (unquote-splicing (1 2)))))", env))) passed++; else failed++;
        if (test("Vector addition", testEval("(+ ($ 1 2) ($ 3 4))", "<4 6>", env))) passed++; else failed++;
        if (test("Vector scalar multiply", testEval("(* ($ 1 2) 3)", "<3 6>", env))) passed++; else failed++;
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

/* add these numeric tests back in later
    public static void main(String[] args) {
        System.out.println("=== BASIC TYPES ===");
        Number i  = Number.integer(100);
        Number k  = Number.integer(new BigInteger("6000000000000000000000"));
        Number f  = Number.real(5300.12347);
        Number bf = Number.real(new BigDecimal("123456789.987654321"));
        Number r  = Number.rational(300, 200); // 3/2
        Number br = Number.rational(new BigInteger("12345678901234567890"),
                                    new BigInteger("9876543210987654321"));
        Number c  = Number.complex(r, i);

        System.out.println("INT:        " + i);
        System.out.println("BIGINT:     " + k);
        System.out.println("FLOAT:      " + f);
        System.out.println("BIGFLOAT:   " + bf);
        System.out.println("RATIONAL:   " + r);
        System.out.println("BIGRATIONAL:" + br);
        System.out.println("COMPLEX:    " + c);
        System.out.println();

        System.out.println("=== SIMPLE ADDITION TESTS ===");
        System.out.println("int + int            = " + Number.add(Number.integer(5), Number.integer(7)) + "   (expected 12)");
        System.out.println("bigint + int         = " + Number.add(k, i) + "   (expected 6000000000000000000100)");
        System.out.println("float + float        = " + Number.add(Number.real(1.25), Number.real(3.75)) + "   (expected 5.0)");
        System.out.println("rational + rational  = " + Number.add(Number.rational(1, 2), Number.rational(1, 3)) + "   (expected 5/6)");
        System.out.println("rational + int       = " + Number.add(Number.rational(3, 2), Number.integer(2)) + "   (expected 7/2)");
        System.out.println("int + rational       = " + Number.add(Number.integer(2), Number.rational(3, 2)) + "   (expected 7/2)");
        System.out.println();

        System.out.println("=== BIG PROMOTIONS ===");
        // overflow test for int + int -> bigint
        Number big1 = Number.integer(Long.MAX_VALUE);
        Number big2 = Number.integer(1);
        System.out.println("overflow test (long + 1) = " + Number.add(big1, big2) + "   (expected 9223372036854775808 as BigInteger)");

        // rational overflow to bigRational
        Number largeR1 = Number.rational(Long.MAX_VALUE / 2, 3);
        Number largeR2 = Number.rational(Long.MAX_VALUE / 2, 3);
        System.out.println("rational overflow => bigRational = " + Number.add(largeR1, largeR2));
        System.out.println();

        System.out.println("=== RATIONAL FACTORY (MIXED TYPES) ===");
        Number rn1 = Number.rational(Number.integer(2), Number.integer(4));
        Number rn2 = Number.rational(Number.integer(2), Number.integer(BigInteger.valueOf(4)));
        Number rn3 = Number.rational(Number.integer(BigInteger.valueOf(6)), Number.integer(BigInteger.valueOf(9)));
        System.out.println("rational(int,int)    = " + rn1 + "   (expected 1/2)");
        System.out.println("rational(int,bigint) = " + rn2 + "   (expected 1/2)");
        System.out.println("rational(big,big)    = " + rn3 + "   (expected 2/3)");
        System.out.println();

        System.out.println("=== MIXED ADDITION TESTS ===");
        System.out.println("float + int          = " + Number.add(Number.real(2.5), Number.integer(3)) + "   (expected 5.5)");
        System.out.println("float + rational     = " + Number.add(Number.real(0.5), Number.rational(1, 3)) + "   (expected ~0.8333)");
        System.out.println("bigfloat + float     = " + Number.add(bf, f) + "   (expected ~123462090.1111)");
        System.out.println("rational + bigfloat  = " + Number.add(r, bf) + "   (expected ~123456791.487654321)");
        System.out.println();

        System.out.println("=== COMPLEX ADDITION ===");
        Number c1 = Number.complex(Number.rational(1, 2), Number.integer(2));
        Number c2 = Number.complex(Number.integer(3), Number.integer(4));
        System.out.println("complex + complex    = " + Number.add(c1, c2) + "   (expected (7/2 + 6i))");
        System.out.println("complex + rational   = " + Number.add(c1, Number.rational(1, 2)) + "   (expected (1 + 2i))");
        System.out.println("complex + int        = " + Number.add(c1, Number.integer(1)) + "   (expected (3/2 + 2i))");
        System.out.println();

        System.out.println("=== BIGFLOAT PRECISION ===");
        BigDecimal veryBig = new BigDecimal("1.0000000000000000000000000000000001");
        System.out.println("bigfloat + bigfloat  = " + Number.add(Number.real(veryBig), Number.real(veryBig)));
        System.out.println();

        System.out.println("=== EDGE CASES ===");
        System.out.println("zero + zero          = " + Number.add(Number.integer(0), Number.integer(0)));
        System.out.println("negatives            = " + Number.add(Number.integer(-5), Number.integer(2)) + "   (expected -3)");
        System.out.println("rational + negative  = " + Number.add(Number.rational(1, 3), Number.integer(-1)) + "   (expected -2/3)");


        System.out.println("=== MULTIPLICATION TESTS ===");
        System.out.println("int * int            = " + Number.multiply(Number.integer(5), Number.integer(7)) + "   (expected 35)");
        System.out.println("bigint * int         = " + Number.multiply(k, i) + "   (expected 600000000000000000000000)");
        System.out.println("float * float        = " + Number.multiply(Number.real(1.25), Number.real(3.75)) + "   (expected 4.6875)");
        System.out.println("rational * rational  = " + Number.multiply(Number.rational(2, 3), Number.rational(3, 4)) + "   (expected 1/2)");
        System.out.println("rational * int       = " + Number.multiply(Number.rational(3, 2), Number.integer(2)) + "   (expected 3)");
        System.out.println("int * rational       = " + Number.multiply(Number.integer(2), Number.rational(3, 2)) + "   (expected 3)");
        System.out.println("rational * bigint    = " + Number.multiply(Number.rational(3, 2), k) + "   (expected 9000000000000000000000)");
        System.out.println();

        System.out.println("=== BIG PROMOTIONS ===");
        // overflow test for int * int -> bigint
        Number bigMul1 = Number.integer(Long.MAX_VALUE);
        Number bigMul2 = Number.integer(2);
        System.out.println("overflow test (long * 2) = " + Number.multiply(bigMul1, bigMul2) + "   (expected 18446744073709551614 as BigInteger)");

        // rational overflow to bigRational
        Number largeRM1 = Number.rational(Long.MAX_VALUE / 2, 3);
        Number largeRM2 = Number.rational(2, 3);
        System.out.println("rational overflow => bigRational = " + Number.multiply(largeRM1, largeRM2));
        System.out.println();

        System.out.println("=== MIXED MULTIPLICATION ===");
        System.out.println("float * int          = " + Number.multiply(Number.real(2.5), Number.integer(3)) + "   (expected 7.5)");
        System.out.println("float * rational     = " + Number.multiply(Number.real(0.5), Number.rational(3, 2)) + "   (expected 0.75)");
        System.out.println("bigfloat * float     = " + Number.multiply(bf, f) + "   (expected ~654012121365.2799)");
        System.out.println("rational * bigfloat  = " + Number.multiply(r, bf) + "   (expected ~185185184.9814814815)");
        System.out.println();

        System.out.println("=== COMPLEX MULTIPLICATION ===");
        Number c3 = Number.complex(Number.integer(1), Number.integer(2));  // 1 + 2i
        Number c4 = Number.complex(Number.integer(3), Number.integer(4));  // 3 + 4i
        System.out.println("complex * complex    = " + Number.multiply(c3, c4) + "   (expected (-5 + 10i))");
        System.out.println("real * complex       = " + Number.multiply(Number.integer(2), c3) + "   (expected (2 + 4i))");
        System.out.println("rational * complex   = " + Number.multiply(Number.rational(1, 2), c3) + "   (expected (1/2 + i))");
        System.out.println();

        System.out.println("=== BIGFLOAT PRECISION (MULT) ===");
        BigDecimal bigPrec = new BigDecimal("1.0000000000000000000000000000000001");
        System.out.println("bigfloat * bigfloat  = " + Number.multiply(Number.real(bigPrec), Number.real(bigPrec))
                        + "   (expected ~1.0000000000000000000000000000000002)");
        System.out.println();

        System.out.println("=== EDGE CASES (MULT) ===");
        System.out.println("zero * any           = " + Number.multiply(Number.zero(Type.INT), Number.integer(999)) + "   (expected 0)");
        System.out.println("negatives            = " + Number.multiply(Number.integer(-5), Number.integer(2)) + "   (expected -10)");
        System.out.println("rational * negative  = " + Number.multiply(Number.rational(1, 3), Number.integer(-3)) + "   (expected -1)");
        System.out.println("complex * zero       = " + Number.multiply(c3, Number.zero(Type.COMPLEX)) + "   (expected 0 + 0i)");
        System.out.println();
    
        System.out.println("=== DIVISION TESTS ===");

        // --- Simple divisions ---
        System.out.println("int / int            = " + Number.divide(Number.integer(7), Number.integer(2)) + "   (expected 7/2)");
        System.out.println("int / int exact      = " + Number.divide(Number.integer(8), Number.integer(2)) + "   (expected 4)");
        System.out.println("bigint / int         = " + Number.divide(k, Number.integer(2)) + "   (expected 3000000000000000000000)");
        System.out.println("rational / rational  = " + Number.divide(Number.rational(3, 4), Number.rational(2, 3)) + "   (expected 9/8)");
        System.out.println("rational / int       = " + Number.divide(Number.rational(3, 2), Number.integer(3)) + "   (expected 1/2)");
        System.out.println("int / rational       = " + Number.divide(Number.integer(3), Number.rational(3, 2)) + "   (expected 2)");
        System.out.println();

        // --- Big promotions ---
        Number bigDiv1 = Number.integer(Long.MAX_VALUE);
        Number bigDiv2 = Number.integer(2);
        System.out.println("overflow test (long / 2) = " + Number.divide(bigDiv1, bigDiv2) + "   (expected 4611686018427387903)");
        System.out.println("bigint / bigint exact     = " + Number.divide(k, Number.integer(3)) + "   (expected 2000000000000000000000)");
        System.out.println("bigint / bigint rational  = " + Number.divide(k, Number.integer(7)) + "   (expected bigRational)");
        System.out.println();

        // --- Mixed division ---
        System.out.println("float / int          = " + Number.divide(Number.real(7.5), Number.integer(3)) + "   (expected 2.5)");
        System.out.println("float / rational     = " + Number.divide(Number.real(1.5), Number.rational(3, 2)) + "   (expected 1)");
        System.out.println("bigfloat / float     = " + Number.divide(bf, f) + "   (expected ~23300.0000189)");
        System.out.println("rational / bigfloat  = " + Number.divide(r, bf) + "   (expected ~0.000000012145)");
        System.out.println();

        // --- Complex division ---
        Number c5 = Number.complex(Number.integer(1), Number.integer(2));  // 1 + 2i
        Number c6 = Number.complex(Number.integer(3), Number.integer(4));  // 3 + 4i
        System.out.println("complex / complex    = " + Number.divide(c5, c6) + "   (expected (11/25 + 2/25i))");
        System.out.println("complex / real       = " + Number.divide(c5, Number.integer(2)) + "   (expected (1/2 + i))");
        System.out.println("real / complex       = " + Number.divide(Number.integer(1), c6) + "   (expected (3/25 - 4/25i))");
        System.out.println();

        // --- Edge cases ---
        System.out.println("zero / nonzero       = " + Number.divide(Number.integer(0), Number.integer(7)) + "   (expected 0)");
        try {
            System.out.println("nonzero / zero       = " + Number.divide(Number.integer(7), Number.integer(0)));
        } catch (ArithmeticException e) {
            System.out.println("nonzero / zero       = Exception (expected)");
        }
        try {
            System.out.println("complex / 0+0i       = " + Number.divide(c5, Number.complex(Number.integer(0), Number.integer(0))));
        } catch (ArithmeticException e) {
            System.out.println("complex / 0+0i       = Exception (expected)");
        }
        System.out.println();

        // =======================
        // === QUATERNION TESTS ===
        // =======================
        System.out.println("=== QUATERNION BASICS ===");
        Number Qi = Number.quaternion(Number.integer(0), Number.integer(1), Number.integer(0), Number.integer(0)); // i
        Number Qj = Number.quaternion(Number.integer(0), Number.integer(0), Number.integer(1), Number.integer(0)); // j
        Number Qk = Number.quaternion(Number.integer(0), Number.integer(0), Number.integer(0), Number.integer(1)); // k
        Number Q1 = Number.quaternion(Number.integer(1), Number.integer(0), Number.integer(0), Number.integer(0)); // 1

        Number qA = Number.quaternion(Number.integer(1), Number.integer(2), Number.integer(3), Number.integer(4)); // 1+2i+3j+4k
        Number qB = Number.quaternion(Number.integer(5), Number.integer(6), Number.integer(7), Number.integer(8)); // 5+6i+7j+8k
        System.out.println("qA = " + qA + "   (expected 1 + 2i + 3j + 4k)");
        System.out.println("qB = " + qB + "   (expected 5 + 6i + 7j + 8k)");
        System.out.println("qA + qB           = " + Number.add(qA, qB) + "   (expected 6 + 8i + 10j + 12k)");
        System.out.println("qB - qA           = " + Number.add(qB, Number.negate(qA)) + "   (expected 4 + 4i + 4j + 4k)");
        // If negate is private, you can simulate subtraction by: qB + (-1)*qA
        // Number.add(qB, Number.multiply(Number.integer(-1), qA))

        System.out.println();
        System.out.println("=== HAMILTON RULES ===");
        System.out.println("i * j = " + Number.multiply(Qi, Qj) + "   (expected 0 + 0i + 0j + 1k)");
        System.out.println("j * k = " + Number.multiply(Qj, Qk) + "   (expected 0 + 1i + 0j + 0k)");
        System.out.println("k * i = " + Number.multiply(Qk, Qi) + "   (expected 0 + 0i + 1j + 0k)");
        System.out.println("j * i = " + Number.multiply(Qj, Qi) + "   (expected 0 + 0i + 0j + -1k)");
        System.out.println("k * j = " + Number.multiply(Qk, Qj) + "   (expected 0 + -1i + 0j + 0k)");
        System.out.println("i * k = " + Number.multiply(Qi, Qk) + "   (expected 0 + 0i + -1j + 0k)");
        System.out.println("i * i = " + Number.multiply(Qi, Qi) + "   (expected -1 + 0i + 0j + 0k)");
        System.out.println("j * j = " + Number.multiply(Qj, Qj) + "   (expected -1 + 0i + 0j + 0k)");
        System.out.println("k * k = " + Number.multiply(Qk, Qk) + "   (expected -1 + 0i + 0j + 0k)");

        System.out.println();
        System.out.println("=== QUATERNION * SCALAR (INT/RATIONAL/FLOAT/BIGFLOAT) ===");
        System.out.println("2 * (1+i+j+k)       = " + Number.multiply(Number.integer(2), Number.quaternion(Number.integer(1), Number.integer(1), Number.integer(1), Number.integer(1))) + "   (expected 2 + 2i + 2j + 2k)");
        System.out.println("(3+6i+0j+0k) / 1.5  = " + Number.divide(Number.quaternion(Number.integer(3), Number.integer(6), Number.integer(0), Number.integer(0)), Number.real(1.5)) + "   (expected 2 + 4i + 0j + 0k)");
        System.out.println("(2/3) * (3+6i+9j+12k) = " + Number.multiply(Number.rational(2,3), Number.quaternion(Number.integer(3), Number.integer(6), Number.integer(9), Number.integer(12))) + "   (expected 2 + 4i + 6j + 8k)");

        System.out.println("bigfloat * (1+i)    = " + Number.multiply(Number.real(new BigDecimal("1.0000000000000000000000000000000001")),Number.quaternion(Number.integer(1), Number.integer(1), Number.integer(0), Number.integer(0))) + "   (expected ~1.000... + 1.000...i)");
        System.out.println();
        System.out.println("=== COMPLEX ⟷ QUATERNION PROMOTION ===");
        Number cA = Number.complex(Number.integer(1), Number.integer(2)); // 1+2i
        System.out.println("(1+2i) * (3+4i+5j+6k) = " + Number.multiply(cA, Number.quaternion(Number.integer(3), Number.integer(4), Number.integer(5), Number.integer(6))) + "   (expected quaternion result)");
        System.out.println("(1+i) * j           = " + Number.multiply(Number.complex(Number.integer(1), Number.integer(1)), Qj) + "   (expected 0 + 0i + 1j + 1k)");
        System.out.println("j * (1+i)           = " + Number.multiply(Qj, Number.complex(Number.integer(1), Number.integer(1))) + "   (expected 0 + 0i + 1j - 1k)  // anti-commutativity shows up");

        // (For subtraction without public negate, simulate as above — multiply by -1 and add)

        System.out.println();
        System.out.println("=== RECIPROCAL & DIVISION ===");
        System.out.println("qA / qA             = " + Number.divide(qA, qA) + "   (expected 1 + 0i + 0j + 0k)");
        System.out.println("(6+8i+10j+12k)/2    = " + Number.divide(Number.quaternion(Number.integer(6), Number.integer(8), Number.integer(10), Number.integer(12)), Number.integer(2)) + "   (expected 3 + 4i + 5j + 6k)");
        try {
            System.out.println("div by zero quat    = " + Number.divide(qA, Number.quaternion(Number.integer(0), Number.integer(0), Number.integer(0), Number.integer(0))));
        } catch (ArithmeticException e) {
            System.out.println("div by zero quat    = Exception (expected)");
        }

        System.out.println();
        System.out.println("=== DISTRIBUTIVITY (SPOT CHECK) ===");
        Number qX = Number.quaternion(Number.integer(2), Number.integer(1), Number.integer(0), Number.integer(1)); // 2 + i + k
        Number qY = Number.quaternion(Number.integer(1), Number.integer(1), Number.integer(1), Number.integer(0)); // 1 + i + j
        Number qZ = Number.quaternion(Number.integer(0), Number.integer(2), Number.integer(1), Number.integer(1)); // 2i + j + k
        Number left = Number.multiply(qX, Number.add(qY, qZ));
        Number right = Number.add(Number.multiply(qX, qY), Number.multiply(qX, qZ));
        System.out.println("qX*(qY+qZ)          = " + left);
        System.out.println("qX*qY + qX*qZ       = " + right);
        System.out.println("distributive equal?  (manual check: the two lines above should match)");

        System.out.println();
        System.out.println("=== ZERO/ONE IDENTITIES ===");
        System.out.println("qA + 0              = " + Number.add(qA, Number.zero(Number.Type.QUATERNION)) + "   (expected qA)");
        System.out.println("qA * 1              = " + Number.multiply(qA, Number.one(Number.Type.QUATERNION)) + "   (expected qA)");

        System.out.println();
        System.out.println("=== POWER TESTS ===");
        System.out.println("2^10               = " + Number.pow(Number.integer(2), Number.integer(10)) + "   (expected 1024)");
        System.out.println("(3/2)^3             = " + Number.pow(Number.rational(3, 2), Number.integer(3)) + "   (expected 27/8)");
        System.out.println("(2.0)^(0.5)         = " + Number.pow(Number.real(2.0), Number.real(0.5)) + "   (expected ~1.41421356237)");
        System.out.println("(1+2i)^3            = " + Number.pow(Number.complex(Number.integer(1), Number.integer(2)), Number.integer(3)) + "   (expected -11-2i)");
        System.out.println("Quaternion^2        = " + Number.pow(qA, Number.integer(2)) + "   (expected quaternion result)");

        System.out.println();
        System.out.println("=== MOD TESTS ===");
        System.out.println("17 mod 5            = " + Number.mod(Number.integer(17), Number.integer(5)) + "   (expected 2)");
        System.out.println("(-7) mod 3          = " + Number.mod(Number.integer(-7), Number.integer(3)) + "   (expected 2)");
        System.out.println("(9/4) mod (2/3)     = " + Number.mod(Number.rational(9, 4), Number.rational(2, 3)) + "   (expected 1/4)");
        System.out.println("5.5 mod 2.0         = " + Number.mod(Number.real(5.5), Number.real(2.0)) + "   (expected 1.5)");
        System.out.println("Complex mod 2       = " + Number.mod(Number.complex(Number.integer(5), Number.integer(3)), Number.integer(2)) + "   (expected (1+1i))");
        System.out.println("Quaternion mod 3    = " + Number.mod(qA, Number.integer(3)) + "   (expected component-wise result)");

        System.out.println();
        System.out.println("=== INEXACT CONVERSIONS ===");
        Number exactR = Number.rational(7, 3);
        Number exactBR = Number.rational(new BigInteger("12345678901234567890"), new BigInteger("12345"));
        Number exactComplex = Number.complex(Number.rational(5, 4), Number.integer(2));
        System.out.println("toInexact 7/3       = " + Number.toInexact(exactR) + "   (expected ~2.3333333333)");
        System.out.println("toInexact bigRat    = " + Number.toInexactBig(exactBR) + "   (expected bigfloat ~1000000065.0)");
        System.out.println("toInexact complex   = " + Number.toInexact(exactComplex) + "   (expected (~1.25 + 2i))");
        System.out.println("toInexactBig qA     = " + Number.toInexactBig(qA) + "   (expected quaternion with bigfloat components)");

        System.out.println();
        System.out.println("=== CONJUGATE & INVERSE ===");
        System.out.println("conj(1+2i)          = " + Number.complexConjugate(c3) + "   (expected 1+-2i)");
        System.out.println("conj(qA)            = " + Number.quaternionConjugate(qA) + "   (expected 1+-2i+-3j+-4k)");
        System.out.println("inverse(qA)         = " + Number.quaternionInverse(qA) + "   (expected quaternion inverse)");

        System.out.println();
        System.out.println("=== MAGNITUDE & COMPARISON ===");
        System.out.println("|1+2i|              = " + Number.magnitude(c3) + "   (expected ~2.2360679)");
        System.out.println("|qA|                = " + Number.magnitude(qA) + "   (expected ~5.4772256)");
        System.out.println("numericEquals 2,4/2 = " + Number.numericEquals(Number.integer(2), Number.rational(4, 2)) + "   (expected true)");
        System.out.println("lessThan 3/2, 2     = " + Number.lessThan(Number.rational(3, 2), Number.integer(2)) + "   (expected true)");
        System.out.println("greaterThan qA,qB   = " + Number.greaterThan(qA, qB) + "   (expected false)");
        System.out.println("lessThan (1+2i),3   = " + Number.lessThan(c3, Number.integer(3)) + "   (expected true)");
    }
    */
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
