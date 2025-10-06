# Getting started 
To run MicroLisp in interactive mode, do `microlisp -i` in your terminal.

```
>>>"hello world!"
"hello world!" 
```

MicroLisp is a dialect of Lisp that runs on the JVM. It is "purely" functional, in this case meaning that mutation is not just disallowed, but is not possible. Like Scheme, microLisp is lexically scoped.

The atomic expressions are: numbers, booleans, integers and the empty list. These evaluate to themselves.
```
>>>1
1 
>>>#t
#t
>>>'()
()
```

Function application uses prefix notation. Some built-in functions include +,-,*,/,^,%, even?, odd?, null?
```
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
```
>>>(+ (* 2 3) 4)   ;(2 * 3) + 4
10
```
Basic arithmetic variadic in arguments
```
>>>(+ 1 2 3)
6 
>>>(* 2 3 4)
24
```
The main(only) built in data structure is the `cons` cell. `cons`ing two expressions together produces a cons cell.
The first element can be accessed with `head` and the second with `tail`.
```
>>>(cons 1 2))
(1 . 2)
>>>(head (cons 1 2))
1
>>>(tail (cons 1 2))
2
```

```
                            -------
 (define cell (cons a b) ->| a | b |
                            -------
                            /     \ 
                  (head cell)    (tail cell)
```

Using cons cells we can build up composite structures like lists.
An improper list ends in a value other than the empty list `'()`.
```
>>>(cons 1 (cons 2 (cons 3 4)))
(1 2 3 . 4)
```
This would be in proper list form: 
```
>>>(cons 1 (cons 2 (cons 3 (cons 4 '()))))
(1 2 3 4)
```
We can access the elements of a list using `head` and `tail` 
```
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
We can use `cond` for conditional expressions
```
(cond ((predicate1) expr)
      ((predicate2) expr)
      ...
      (else expr))
```
For example
```
>>>(cond ((even? 2) "first branch") 
         (else "second branch"))

"first branch"
```
`cond` only requires that one predicate is #t, so this is valid
```
>>>(cond (else "single clause"))
"single clause"
```
But this will throw a runtime exception
```
>>>(cond (#f "this wont evaluate"))
Exception in thread "main" java.lang.RuntimeException: cond: no true clause and no else clause
```
So it is best practice to always have an else clause, unless the goal is to exit ungracefully when no clauses evaluate true

