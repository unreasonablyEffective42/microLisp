(define foldl
  (lambda (fn z xs)
    (cond ((null? xs) z)
          (else (foldl fn (fn (head xs) z) (tail xs))))))

(define reverse (lambda (xs) (foldl cons '() xs)))

(define lcomp 
  (lambda (n m)
    (let loop ((n n) (m m) (xs '()))
      (cond ((eq? m n) (reverse (cons m xs)))
            (else (loop n (- m 1) (cons m xs)))))))

(head (map (lambda (x) (^ x 2)) (lcomp 1 1000000)))
