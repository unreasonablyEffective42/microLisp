(define foo (lambda (x) (+ x 1)))

(define bar (lambda (x) (cond ((> x 2) "BANG") (else "BOOM"))))

(define fact (lambda (x) (fact-helper x 1)))

(define fact-helper (lambda (n a) (cond ((eq? n 0) a) (else (fact-helper (- n 1) (* a n))))))


(define fibs (lambda (n) (fibshelper 0 1 n '())))

(define fibshelper 
  (lambda (a b n xs)
    (cond ((eq? n 0) (reverse (cons a xs)))
          (else (fibshelper b (+ a b) (- n 1) (cons a xs))))))

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
    (let loop ((rest xs) (acc '()))
      (cond
        ((null? rest) (reverse acc))
        (else
          (lets ((f  (head rest))
                 (fs (tail rest)))
            (loop (filter (lambda (q) (not (eq? 0 (% q f)))) fs)
                  (cons f acc))))))))


