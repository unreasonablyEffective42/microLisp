(define map 
  (lambda (fn xs)
    (let loop ((xs xs) (ys '()))
      (cond ((null? xs) (reverse ys))
            (else (loop (tail xs) (cons (fn (head xs)) ys)))))))

(define filter
  (lambda (pred xs)
    (let loop ((xs xs) (ys '()))
      (cond ((null? xs) (reverse ys))
            ((pred (head xs)) (loop (tail xs) (cons (head xs) ys)))
            (else (loop (tail xs) ys))))))

(define foldr (lambda (fn z xs) (reverse (foldl fn z xs))))

(define foldl
  (lambda (fn z xs)
    (cond ((null? xs) z)
          (else (foldl fn (fn (head xs) z) (tail xs))))))

(define reverse (lambda (xs) (foldl cons '() xs)))

(define lcomp 
  (lambda (n m)
    (let loop ((n n) (m m) (xs '()))
      (cond ((eq? m n) (cons m xs))
            (else (loop n (- m 1) (cons m xs)))))))




