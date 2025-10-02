MicroLisp is a lightweight lisp interpreter loosely based on the syntax and semantics of the Scheme programming language.

This is an honors project for my Java programming class at CCD.

This interpreter currently supports int32, and their arithmetic operations, string literals, lists, functions and recursion.

The syntax and semantics almost entirely follow from Scheme. Some differences include: 
- `car` and `cdr` are replaced with `head` and `tail` 
- `(define (fnName arg1 arg2 ...) functionBody)` is instead `(define fnName (lambda (arg1 arg2 ...) functionBody))`

-   Application of raw lambdas do not evaluate currently at the moment, `((lambda (x) (+ x 1)) 1)' -> 2` is the expected behavior, instead application of lambda expressions directly just return the closure token for the evaluator
-   arithmetic operations take `varargs`, just like scheme, but using say `+` in `(foldr + 0 '(1 2 3 4 5))` does not work, you have to turn the expression into a binary operation like 
	- `(define add (lambda (x y) (+ x y)))` then `(foldr add 0 '(1 2 3 4 5))`

To build, clone the repository 
`git clone [https://github.com/unreasonablyeffective42/microlisp` 
and from the directory ./MicroLisp, enter: `./BUILD` 
Then you can run from a new terminal with `microlisp` Optionally you can pass it a file to load as well `microlisp file.scm`

To build it yourself, navigate to the MicroLisp directory then `javac -d ./out ./src/*.java` To run just `java -cp ./out/ MicroLisp` + optional files to be loaded.
