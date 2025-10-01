(define foo (lambda (x) (+ x 1)))

(define bar (lambda (x) (cond ((> x 2) "BANG") (else "BOOM"))))

(define fact (lambda (x) (fact-helper x 1)))

(define fact-helper (lambda (n a) (cond ((eq? n 0) a) (else (fact-helper (- n 1) (* a n))))))

(define map 
  (lambda (fn lst)
    (cond ((null? lst) '() )
          (else (cons (fn (head lst)) (map fn (tail lst)))))))
