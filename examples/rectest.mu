;testing deep recursion for overflow
(define main
  (lambda (s)
    (let loop ((a 0))
      (cond ((eq? a 100000000) (print a))
             (else (loop (+ a 1)))))))
