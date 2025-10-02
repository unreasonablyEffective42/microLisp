(define foo (lambda (x) (+ x 1)))

(define bar (lambda (x) (cond ((> x 2) "BANG") (else "BOOM"))))

(define fact (lambda (x) (fact-helper x 1)))

(define fact-helper (lambda (n a) (cond ((eq? n 0) a) (else (fact-helper (- n 1) (* a n))))))

(define map 
  (lambda (fn xs)
    (cond ((null? xs) xs)
          (else (cons (fn (head xs)) (map fn (tail xs)))))))

(define filter
  (lambda (pred xs)
    (cond ((null? xs) xs)
          ((pred (head xs)) (cons (head xs) (filter pred (tail xs))))
          (else (filter pred (tail xs))))))

(define foldr
  (lambda (fn z xs)
    (cond ((null? xs) z)
          (else (fn (head xs) (foldr fn z (tail xs))))))

(define foldl
  (lambda (fn z xs)
    (cond ((null? xs) z)
          (else (foldl fn (fn (head xs) z) (tail xs))))))

(define reverse (lambda (xs) (foldl cons '() xs)))

(define fibs (lambda (n) (fibshelper 0 1 n)))

(define fibshelper 
  (lambda (a b n)
    (cond ((eq? n 0) (cons a '() ))
          (else (cons a (fibshelper b (+ a b) (- n 1)))))))

