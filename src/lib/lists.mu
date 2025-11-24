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

(define zip
  (lambda (xs ys)
    (let loop ((xs xs) (ys ys) (zs '()))
      (cond ((or (null? xs) (null? ys)) (reverse zs))
            (else (loop (tail xs) (tail ys) (cons (list (head xs) (head ys)) zs)))))))

(define zip-3 
  (lambda (xs ys ws)
    (let loop ((xs xs) (ys ys) (ws ws) (zs '()))
      (cond ((or (or (null? xs) (null? ys)) (null? ws)) (reverse zs))
            (else (loop (tail xs) (tail ys) (tail ws) (cons (list (head xs) (head ys) (head ws)) zs)))))))


(define zip-with
  (lambda (fn xs ys)
    (let loop ((xs xs) (ys ys) (zs '()))
      (cond ((or (null? xs) (null? ys)) (reverse zs))
            (else (loop (tail xs) (tail ys) (cons (fn (head xs) (head ys)) zs)))))))

(define append
  (lambda (xs ys)
    (let loop ((xs xs) (ys ys) (zs '()))
      (cond ((and (null? xs) (null? ys)) (reverse zs))
            ((and (null? xs) (list? ys)) (loop xs (tail ys) (cons (head ys) zs)))
            ((null? xs) (loop xs '() (cons ys zs)))
            (else (loop (tail xs) ys (cons (head xs) zs)))))))

(define flatten
  (lambda (xs)
    (let loop ((xs xs) (zs '()))
      (cond ((null? xs) (reverse zs))
            ((list? (head xs)) (loop (append (head xs) (tail xs)) zs))
            (else (loop (tail xs) (cons (head xs) zs)))))))

(define reverse (lambda (xs) (foldl (lambda (x y) (cons y x)) '() xs)))

(define lcomp 
  (lambda (n m)
    (let loop ((n n) (m m) (xs '()))
      (cond ((eq? m n) (cons m xs))
            (else (loop n (- m 1) (cons m xs)))))))

(define interp
  (lambda (str end stp)
    (interpp str end stp '())))

(define interpp 
  (lambda (str end stp xs)
    (cond ((< str end) (interpp (+ str stp) end stp (cons str xs)))
          (else (reverse xs)))))

(define list-ref
    (lambda (xs n)
      (cond ((eq? n 0) (head xs))
            (else (list-ref (tail xs) (- n 1))))))



