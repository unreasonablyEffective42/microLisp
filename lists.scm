(define map 
  (lambda (fn xs)
    (cond ((null? xs) xs)
          (else (cons (fn (head xs)) (map fn (tail xs)))))))

(define filter (lambda (pred xs) (filterh pred xs '())))

(define filterh
  (lambda (pred xs ys)
    (cond ((null? xs) (reverse ys)) 
          ((pred (head xs)) (filterh pred (tail xs) (cons (head xs) ys)))
          (else (filterh pred (tail xs) ys))))) 

(define foldr
  (lambda (fn z xs)
    (cond ((null? xs) z)
          (else (fn (head xs) (foldr fn z (tail xs)))))))

(define foldl
  (lambda (fn z xs)
    (cond ((null? xs) z)
          (else (foldl fn (fn (head xs) z) (tail xs))))))

(define reverse (lambda (xs) (foldl cons '() xs)))


