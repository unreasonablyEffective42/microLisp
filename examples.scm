(define foo (lambda (x) (+ x 1)))

(define bar (lambda (x) (cond ((> x 2) "BANG") (else "BOOM"))))

(define fact (lambda (x) (fact-helper x 1)))

(define fact-helper (lambda (n a) (cond ((eq? n 0) a) (else (fact-helper (- n 1) (* a n))))))


(define fibs (lambda (n) (fibshelper 0 1 n)))

(define fibshelper 
  (lambda (a b n)
    (cond ((eq? n 0) (cons a '() ))
          (else (cons a (fibshelper b (+ a b) (- n 1)))))))

(define pair
  (lambda (x y)
    (lambda (m) (m x y))))

(define first
  (lambda (z)
    (z (lambda (p q) p))))

(define second
  (lambda (z)
    (z (lambda (p q) q))))

