#h1 Getting started 

MicroLisp is a dialect of Lisp that runs on the JVM. It is "purely" functional, in this case meaning that mutation is not just disallowed, but is not possible. Like Scheme, microLisp is lexically scoped.

The atomic expressions are: numbers, booleans, integers and the empty list. These evaluate to themselves.
```
>>>1
1 
>>>#t
#t
>>>'()
()
>>>"hello world!"
"hello world!" 
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
By chaining `cons`s, we have built up this structure:
``` 
(define xs (cons 1 (cons 2 (cons 3 (cons 4 '())))))
                   ------- 
                  | 1 |   |
                   ------- 
                   /     \
          (head xs)      ------- 
                        | 2 |   |
                         ------- 
                       /       \
                      /        ------- 
          (tail xs)  -        | 3 |   |
                      \        ------- 
                       \             \
                        \            ------- 
                         \          | 4 |'()|
                                     ------- 

```

