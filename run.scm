(define foo ;comment  
  (lambda (x)
    (lambda (y)
      (+ x y)))) ;blah

(let ((f (foo 1)))
  (f 2))

