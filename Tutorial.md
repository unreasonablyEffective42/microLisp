# Getting started 
To run MicroLisp in interactive mode, do `microlisp -i` in your terminal.

```Scheme
>>>"hello world!"
"hello world!" 
```

MicroLisp is a dialect of Lisp that runs on the JVM. It is "purely" functional, in this case meaning that mutation is not just disallowed, but is not possible. Like Scheme, microLisp is lexically scoped.
Everything in MicroLisp is an expression. We have atomics, which are the simplest, while all others are composites, and will be bound with parentheses.
The atomic expressions are: numbers, booleans, integers and the empty list. These evaluate to themselves.
```Scheme
>>>1
1 
>>>#t
#t
>>>'()
()
```
Function application is a composite expression, Function application uses prefix notation. 
Some built-in functions include +,-,*,/,^,%, even?, odd?, null?, eq?, >, <
```Scheme
>>>(+ 1 2)
3
>>>(* 4 10)
40
>>>(even? 6)
#t
>>>(null? 3)
#f
```
We can compose functions with nesting 
```Scheme
>>>(+ (* 2 3) 4)   ;(2 * 3) + 4
10
```
Basic arithmetic is variadic in arguments
```Scheme
>>>(+ 1 2 3)
6 
>>>(* 2 3 4)
24
```
The main(only) built in data structure is the `cons` cell. `cons`ing two expressions together produces a cons cell.
The first element can be accessed with `head` and the second with `tail`.
```Scheme
>>>(cons 1 2)
(1 . 2)
>>>(head (cons 1 2))
1
>>>(tail (cons 1 2))
2
```

```
(define cell (cons a b))

              -------
      cell ->| a | b |
              -------
              /     \ 
      (head cell)    (tail cell)
```

Using cons cells we can build up composite structures like lists.
An improper list ends in a value other than the empty list `'()`.
```Scheme
>>>(cons 1 (cons 2 (cons 3 4)))
(1 2 3 . 4)
```
This would be in proper list form: 
```Scheme
>>>(cons 1 (cons 2 (cons 3 (cons 4 '()))))
(1 2 3 4)
```
We can access the elements of a list using `head` and `tail` 
```Scheme 
>>> (define xs (cons 1 (cons 2 (cons 3 (cons 4 '())))))
>>> xs
(1 2 3 4)
>>> (head xs)
1 
>>> (tail xs)
(2 3 4)
```
By chaining `cons`, we have built up this structure:
``` 
(define xs (cons 1 (cons 2 (cons 3 (cons 4 '())))))
                     ------- 
    (head xs) -> 1  | 1 |   |
                     ------- 
    |                      \
    |                     ------- 
    |                    | 2 |   | 2 <- (head (tail xs))
    |                     ------- 
(tail xs)  |                    \
    |      |                    ------- 
    |      |                   | 3 |   | 3 <- (head (tail (tail  xs)))
    | (tail (tail xs))          ------- 
    |      |            |             \
    |      |            |             ------- 
    |      | (tail (tail (tail xs))) | 4 |'()|  4 <- (head (tail (tail (tail xs)))
    |      |            |             ------- 
                                              '() <- (tail (tail (tail (tail xs))))
```
We have much nicer syntax for creating lists using `list` or `quote`
```Scheme
>>>(list 1 2 3 4)
(1 2 3 4)
>>>(quote (1 2 3 4))
(1 2 3 4)
>>>'(1 2 3 4)
(1 2 3 4)
```
We will talk more about `quote` and `'`


We can use `cond` for conditional expressions
```Scheme
(cond ((predicate1) expr)
      ((predicate2) expr)
      ...
      (else expr))
```
For example
```Scheme
>>>(cond ((even? 2) "first branch") 
         (else "second branch"))

"first branch"
```
`cond` only requires that one predicate is #t, so this is valid
```Scheme
>>>(cond (else "single clause"))
"single clause"
```
But this will throw a runtime exception
```Scheme 
>>>(cond (#f "this wont evaluate"))
Exception in thread "main" java.lang.RuntimeException: cond: no true clause and no else clause
```
So it is best practice to always have an else clause, unless the goal is to exit ungracefully when no clauses evaluate true

Functions are created with `lambda`.
```Scheme
;single argument function
(lambda (arg) body-expr)

;multiple arguments 
(lambda (arg1 arg2 ... argn) body-expr)
```
Lambdas are applied just like any other function, by having it at the head of a list 
```Scheme
;+ is at the head of the list 
>>>(+ 1 2)
3 
>>>((lambda (x) (* 2 (+ x 1))) 1) ; -> (lambda (1) (*2 (+ x 1))) -> (* 2 (+ 1 1 )) -> 4
4
```
Using `define`, we can add a name to a function
```Scheme
(define foo 
  (lambda (x) (* 2 (+ x 1))))

>>>(foo 1)
4 
>>>(foo 2) 
6
```
Lambdas can only have a single body expression. To have multiple expressions evaluate in order, use `do`
```Scheme
;(print expr) is a built in function that prints the expression to the terminal, more later on specifics

;This is not allowed 
(define foo 
  (lambda (x)
    (print x) <-expr 1 
    (+ x 1))) <-expr 2

;use do
(define foo 
  (lambda (x) 
    (do (print x)
        (+ x 1)))) 

>>> (foo 1)
1 
2
>>>
```
Because (do expr1 expr2 ... exprn) is within a pair of parentheses, it is just a single expression. 
It will evaluate each expression in order, discarding their values, until the last expression which will return.
```Scheme
(do
  (print 2)      ;prints 2, returns "", discard return value
  (* 2 10)       ;evaluates to 20, is discarded 
  (^ 2 4))        ;evaluates to 16, is returned
2
16 
>>>
```
We can use any expression as the body expression, so far we know `cons` `list` and `cond` 
```Scheme
(define bar
  (lambda (x)
    (cond ((eq? x 2) "BANG!")
          (else "BOOM!")))) 
```

```
>>>(bar 8)
"BOOM!"
>>>(bar 2)
"BANG!"
```
Unlike most languages, there are no built in constructs for repeating an action such as `while`,`for`,`for-each` etc.
Instead to repeat actions we use recursion 
```Scheme
(define baz
  (lambda (x)
    (cond ((< x 10) ; <- we need a base condition or the function will enter an infinite recursion
            (do
              (print x)
              (baz (+ x 1)))) ; <- Calling the function recursively
          (else "done")))) 
```

```
>>>(baz 0)
0
1
2
3
4
5
6
7
8
9
"done"
```
The cannonical example of a recursive function is `factorial`
```Scheme
(define factorial
  (lambda (n)
    (cond ((eq? n 1) 1)
          (else (* n (factorial (- n 1)))))))
```

```
>>>(factorial 3)
6
>>>(factorial 5)
120
>>>(factorial 10000)
Exception in thread "main" java.lang.StackOverflowError
```
Oh no!, a loop of only 10,000 is puny compared to what we can do with for and while in other languages.
Luckily we have a workaround in MicroLisp. First lets look at the call stack for one that does not overflow:
```
(factorial 5) <- 5 > 1, so recurse 
(* 5 (factorial 4)) <- 4 > 1, recurse 
(* 5 (* 4 (factorial 3))) <- 3 > 1, recurse 
(* 5 (* 4 (* 3 (factorial 2)))) <- 2 > 1, recurse 
(* 5 (* 4 (* 3 (* 2 (factorial 1))))) 1 == 1, return 1 
(* 5 (* 4 (* 3 (* 2 1))))
(* 5 (* 4 (* 3 2))) 
(* 5 (* 4 6))
(* 5 24)
120
```
So recursion is pretty neat, but as you can see, each call adds to the size, as we don't get to start unwinding until the very end,
meaning that `(factorial 10000)` is attempting to 
```
(* 10000 (* 9999 (* 9998 (* ... (* 2 1) ... ))))
```
But that overflows the stack because each outer nesting takes up valuable stack space. 
Lets look at a different way to write `factorial`
```Scheme
(define factorial (lambda (n) (factorial-helper n 1)))

(define factorial-helper
  (lambda (n a)
    (cond ((eq? n 1) a)
          (else (factorial-helper (- n 1) (* a n))))))
```

```
>>>(factorial 10000)
2846259680917054518906413212119868890148051401702799230794179994274411340003764443772990786757784775815884062142317528830042339940153518739052421161382716174819824... ; plus 35496 more digits
```
Wow that's a huge number! We also didn't overflow the stack! This is because `factorial-helper` is in tail form. 
A tail call in a function is when the last part of a function is itself an expression, and when the last expression is
a recursive call back to itself, then a function is said to be tail recursive. Looking at the call stack
```
(factorial-helper 5 1)   ->  5 > 1, recurse
(factorial-helper 4 5)   ->  4 > 1, recurse 
(factorial-helper 3 20)  ->  3 > 1, recurse 
(factorial-helper 2 60)  ->  2 > 1, recurse 
(factorial-helper 1 120) -> n == 1, return a 
                 /    \
                n      a 
```
Okay, that is pretty neat huh? Because we pass forward the value each time we recursively call `factorial-helper`, we don't grow the stack.
In most languages, this still would not work as each recursive all adds to the stack, even though there is no unwinding needed to be done,
but luckily, MicroLisp performs 'Tail Call Optimization'. TCO takes what would be repeated additions to the stack, and turns it into constant memory space usage in the heap. 
#lets sweeten our syntax with `let` and `lets`
`define` feels heavy, and should only really be used for top level definitions, sometimes we want to just
hold onto a value(s) with a name for a moment. 
```Scheme
(let ((x1 expr1)
      (x2 expr2)
      ...
      (xn exprn))
  (body expression))
```
Gives us exactly that, we start with a list of labels, and expressions we want evaluated and then bound to the labels,
which we can then use in the body expression. 
```Scheme 
(let ((x 3)
      (y 4))
  (+ x y)) 

>7
```
`let` is really just syntactic sugar for `lambda`, because `((lambda (x) body) 2)` binds 2 to x in the body.
The above example is equivalent to:
```Scheme
((lambda (x y)
   (+ x y))
 3 4)
```
`let` just makes it much more convenient

`let` performs bindings in parallel. 
```Scheme
(let ((x 2)  ; <- x = 2 
      (y (+ x 3))) ; <- x is not yet bound in this context, so we throw an error
  (* y 4)) ; <- we want 20, but this is an error 
```
if we turn this into the equivalent lambda expression 
```Scheme 
((lambda (x y) ; -
    (* y 4)    ; |-this is the scope x and y are bound in 
  )            ; -
2 (+ x 3))  ;<- x is not bound here, so we get an error
```
What we need is `lets`. Instead of doing parallel binding, `lets` does binding sequentially
```Scheme
(lets ((x 2)
       (y (+ x 3)))
  (* y 4))

20
```
This is equivalent to nested `let`
```Scheme
(let ((x 2))
  (let (y (+ x 3))
    (* y 4)))
```
Which expands to 
```Scheme
((lambda (x)
  ((lambda (y)
    (* y 4))
  (+ x 3)))
2)
```
Using let and lets allows us to write functional programs in an imperative style 
```Scheme 
(lets ((x 2)              ; x = 2
       (y (* x 3))        ; y = 6 
       (z (+ 1 (^ y 2)))) ; z = 37
  (cond ((even? z) "Success!")
        (else "fail"))) 

"fail"
```
Doing explicit recursions while powerful, might be visually hard to parse, so when we want something like a loop, 
`let` is great syntactic sugar as well, letting us write recursion that looks like a loop 
```Scheme 
(let loop ((v1 b1) 
           (v2 b2)
           ...
           (vn bn))
  (do 
    (expr1)
    (expr2)
    ...
    (loop v1 v2 ... vn)))
```
We still only have one body expression, so we need some kind of expression from which we can call loop again
Named `let` can be named anything, here is a more concrete example 
```Scheme
(let foo ((x 0) 
          (y '()))
  (cond ((eq? x 10) y)
        (else (foo (+ x 1) (cons x y)))))

-> (9 8 7 6 5 4 3 2 1)
```
As you can see, we can name the let anything we want to 
Let also gives us a good way to examine how lexical scoping works 
```Scheme 
(let ((x 1))     ; <- this is the outermost scope, here x is bound to 1 
  (do      
    (print x)    ; x = 1 so we print 1 
    (let ((x 2)) ; <- Making a new lexical scope, where x is bound to 2
      (print x)  ; printing x prints 2 
    )            
    (print x)))  ; we have exited the inner lexical scope, so x is now bound to 1 and we print 1 again 
;if we attempted to (print x) from here, we would get a runtime exception: x is not bound
;because we are outside of the scope of the let block

1 
2 
1 

```
MicroLisp supports first class functions, meaning that we can use functions as arguments and return them as values.
```Scheme
(define foo 
  (lambda (x)  ; <-  foo takes one argument 'x'
    (lambda (y) ; <-  returns a new lambda of argument 'y' with x bound to a value 
      (+ x y)))) 
```
If we define the above, and then:
```
>>>(define f (foo 1))
>>>(f 2)
3 
```
This works because (foo 1) -> (lambda (y) (+ 1 y))
```Scheme 
(define f (foo 1)) -> bind f to (lambda (y) (+ 1 y))
             ↓                ↑
          returns -> (lambda (y) (+ 1 y))

(f 2) -> (lambda (2) (+ 1 y)) -> 3 
```
We can also pass functions as inputs to other functions
```Scheme 
(define map
  (lambda (fn xs)
    (cond ((null? xs) '())                    ;if xs is empty, finish the recursion
          (else (cons (fn (head xs)) (map fn (tail xs)))))))
;                          |                     |
;         apply fn to the head of xs     recurse with the rest of the list 
```
Then if we :
```
>>>(map even? '(1 2 3 4 5 6))
(#f #t #f #t #f #t)
```
Using higher order functions like map is an effective strategy for solving problems
in a functional language like MicroLisp. Another common pattern we can solve is 
filtering a list 
```Scheme 
(define filter
  (lambda (pred xs)
    (cond ((null? xs) '())    ;if we reach the end of the list, end the recursion
          ((pred (head xs))   ;if true, keep the head and process the rest of the list 
           (cons (head xs) (filter pred (tail xs))))
          (else (filter pred (tail xs)))))) ;discard the head and process the rest
```

```
>>>(filter even? '(1 2 3 4 5 6))
(2 4 6)
```
We can even combine the two
```
>>>(filter even? (map (lambda (x) (^ x 2)) '(1 2 3 4 5 6)))
(4 16 36)
```
The last higher order function pattern we will look at is `foldl` and `foldr`
We will use `:` as a shorthand for `cons`
```Scheme 
(define xs '(1 2 3 4 5))
;This is one way we can think about the list xs
 
   cons
   /  \
  1  cons
     /  \
    2  cons 
       /  \
      3  cons
         /  \ 
        4  cons 
           /  \ 
          5   '()  
```
In both map and filer, we had to visit each element of the
list and do something with it. Folding a list is a way to 
traverse a list and apply a function to its members 
left folding, or `foldl`, starts at the left, or front of a list 
and consumes the elements one by one with a binary function,
where the result of the previous application is passed forward 
to be used as the second argument to the function, going until
we reach the end of the list. 
```Scheme 
;foldl takes a binary (two argument) function fn
;a 'default' value z, and a list to fold over, xs
;and then fold from the left most element of the list,
;with the first call consuming the default value and
;the head, and then using the result as the default for 
;each recursion after 

(define foldl
  (lambda (fn z xs)
    (cond ((null? xs) z)
          (else (foldl fn (fn z (head xs)) (tail xs))))))
```

```
>>>(foldl + 0 '(1 2 3 4 5)) ;sum up the elements of a list 
15
>>>(foldl cons '() '(1 2 3 4 5))
(((((() . 1) . 2) . 3) . 4) . 5)
>>>(foldl (lambda (x y) (cons y x)) '() '(1 2 3 4 5))
(5 4 3 2 1)
```

To better understand foldl, we can see how it transforms the structure of a list
```
(foldl + 0 '(1 2 3 4 5))

   cons                       +
   /  \                      / \ 
  1  cons                   +   5
     /  \                  / \
    2  cons       ->      +   4
       /  \              / \
      3  cons           +   3 
         /  \          / \
        4  cons       +   2 
           /  \      / \
          5   '()   0   1 

(foldl cons '() '(1 2 3 4 5))

   cons                      cons 
   /  \                      /  \ 
  1  cons                   cons 5
     /  \                  /  \
    2  cons       ->     cons  4
       /  \              /  \
      3  cons          cons  3 
         /  \          /  \
        4  cons      cons  2 
           /  \      /  \
          5   '()  '()   1 

```
`foldl` is inherently tail recursive, while its sister `foldr` is not.
```Scheme 
(define foldr
  (lambda (fn z xs)
    (cond ((null? xs) z)
          (else (fn (head xs) (foldr fn z (tail xs)))))))
```

```
>>>(foldr + 0 '(1 2 3 4 5))
15
>>>(foldr cons '() '(1 2 3 4 5))
(1 2 3 4 5)
```
Even though the naive implementation of `foldr` is not tail recursive 
we can define it in terms of `foldl`
```Scheme 
(define foldr 
  (lambda (f z xs)
    ((foldl (lambda (g x) 
              (lambda (acc) (g (f x acc))))
            (lambda (acc) acc)
            xs)
     z)))
```
And because `foldl` is tail recursive, so is this definition of `foldr` 
We can also implement `map` and `filter` in terms of `foldr`
```Scheme 
(define map 
  (lambda (f xs)  
    (foldr (lambda (x acc) (cons (f x) acc))
           '()
           xs))) 
 
(define filter 
  (lambda (pred xs)
    (foldr (lambda (x acc)
             (cond ((pred x) (cons x acc))
                   (else acc)))
           '()
           xs)))
```
Though these implementations are not as efficient as they could be.
Check out their tail recursive definitions in `lists.scm` 

Higher order functions can even be used to make data types,
while the internal representation of `cons` is not in terms
of functions, we can implement `cons` with nothing but `lambda` 
```Scheme 
(define cons
  (lambda (x y)
    (lambda (m) (m x y))))

(define head
  (lambda (z)
    (z (lambda (p q) p))))

(define tail
  (lambda (z)
    (z (lambda (p q) q))))
```
Walking through this ...
```
(cons 'a 'b) returns -> (lambda (m) (m 'a 'b'))
(head (cons 'a 'b)) returns -> 'a
Expand     -> ((lambda (z) (z (lambda (p q) p))) (lambda (m) (m 'a 'b)))
Substitute -> ((lambda (m) (m 'a 'b)) (lambda (p q) p))
Substitute -> ((lambda (p q) p) 'a 'b)
'a

(tail (cons 'a 'b)) returns -> 'a
Expand     -> ((lambda (z) (z (lambda (p q) p))) (lambda (m) (m 'a 'b)))
Substitute -> ((lambda (m) (m 'a 'b)) (lambda (p q) q))
Substitute -> ((lambda (p q) q) 'a 'b)
'b
```
Pretty magical eh? 
In fact, we can even define numbers, boolean values, conditional statements, etc.
just in terms of lambda application.

So we have to talk about `quote`. Quote is a special form that lets us take a Microlisp expresssion
and instead of evaluating it, return it back unchanged. This is almost like returning a string with
literal expression in it, but it is more subtle. 
```
>>>(define q (quote (+ 1 2)))
>>>q 
(+ 1 2)
>>>(head q)
+ 
>>>(tail q)
(1 2)
```
If `quote` just returned the string "(+ 1 2)" then:
```
>>>(head q)
"("
>>>(tail q)
"+ 1 2)"
```
So `quote` and `'` preserve the structure of the quoted expression while still processing it 
We can use `eval` to evaluate a quoted expression 
```
>>>q 
(+ 1 2)
>>>(eval q)
3
```
When we use `'()`, we are quoting the empty list (), so that we can pass it as a value instead of evaluating it.
Because the literal structure of MicroLisp code takes the form of nested linked lists, and MicroLisp is made to 
process lists using `head` and `tail`,  we get the neat property of `Homoiconicity`, which is that the code itself
is exactly the kind of data we process. This means that MicroLisp programs can easily modify another MicroLisp program.
Using `quote` and `unquote`, we can do structural transformations on code, and then evaluate them after all while in 
the runtime. 
