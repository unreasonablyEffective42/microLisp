(import lists)

(define list->string
  (lambda (chars)
    (foldl (lambda (acc ch) (cons ch acc)) "" (reverse chars))))

(define string->list
  (lambda (xs)
    (let loop ((xs xs) (rs '()))
      (cond ((null? xs) (reverse rs))
            (else (loop (tail xs) (cons (head xs) rs)))))))

(define substring 
  (lambda (str i j)
    (cond ((< i 0) (do (print "error, lower index must be greater than or equal to 0") #f))
          ((> j (- (length str) 1)) (do (print "error, upper index out of bounds") #f))
          (else 
            (let loop ((str str) (n 0))
              (cond ((< n i) (loop (tail str) (+ n 1)))
                    (else (let loop ((str str) (res '()) (n 0))
                            (cond ((eq? n j) (list->string (reverse (cons (head str) res))))
                                  (else (loop (tail str) (cons (head str) res) (+ n 1))))))))))))

(define string-append
  (lambda (str1 str2)
    (list->string (append str1 str2))))

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

