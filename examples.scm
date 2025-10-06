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

(define sieve
  (lambda (xs)
    (lets ((loop (lambda (rest acc)
                   (cond
                     ((null? rest) (reverse acc))
                     (else (lets ((f  (head rest))
                                  (fs (tail rest)))
                             (loop (filter (lambda (q) (not (eq? 0 (% q f)))) fs)
                                   (cons f acc))))))))
      (loop xs '()))))

(define lcomp (lambda (n m) (lcomphelper n m '())))

(define lcomphelper 
  (lambda (n m xs)
    (cond ((eq? m n) (cons m xs))
          (else (lcomphelper n (- m 1) (cons m xs))))))

(((lambda (x) 
    (lambda (y) (+ x y)))
  2) 
 3)
