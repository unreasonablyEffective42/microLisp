





((lambda (x) x) 1)


((lambda (x) (+ x 2)) 3)


((lambda (x)
   ((lambda (y) (+ x y)) 5))
 10)


(define make-adder
  (lambda (n)
    (lambda (x) (+ x n))))

(define add5 (make-adder 5))
(add5 10)


((lambda (a b) (+ a b)) 2 3)


((make-adder 3) 7)
