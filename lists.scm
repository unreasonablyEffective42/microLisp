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
