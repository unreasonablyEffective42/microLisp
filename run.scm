(define baz
  (lambda (x)
    (cond ((< x 10) 
            (do
              (print x)
              (baz (+ x 1))))
          (else "done")))) 

(define factorial (lambda (n) (factorial-helper n 1)))

(define factorial-helper
  (lambda (n a)
    (cond ((eq? n 1) a)
          (else (factorial-helper (- n 1) (* a n))))))

