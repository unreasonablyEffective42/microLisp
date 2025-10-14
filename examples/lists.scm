(define map   ;apply fn to each element of xs
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

(define foldl
  (lambda (fn z xs)
    (cond ((null? xs) z)
          (else (foldl fn (fn z (head xs)) (tail xs))))))

(define foldr 
  (lambda (f z xs)
    ((foldl (lambda (g x) 
              (lambda (acc) (g (f x acc))))
            (lambda (acc) acc)
            xs)
     z)))

(define reverse (lambda (xs) (foldl (lambda (x y) (cons y x)) '() xs)))

(define lcomp 
  (lambda (n m)
    (let loop ((n n) (m m) (xs '()))
      (cond ((eq? m n) (cons m xs))
            (else (loop n (- m 1) (cons m xs)))))))

(define chars->string
  (lambda (chars)
    (foldl (lambda (acc ch) (cons ch acc)) "" (reverse chars))))

(define words
  (lambda (str)
    (let loop ((word "")
               (ws '())
               (str str)) 
        (cond ((null? str) (map chars->string (map reverse (reverse (cons word ws)))))
              ((or (eq? (head str) " ")
                   (eq? (head str) "\n"))
               (loop "" (cons  word ws) (tail str)))
              (else (loop (cons (head str) word) ws (tail str)))))))




