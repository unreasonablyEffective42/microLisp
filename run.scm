(define map
  (lambda (fn xs)
    (cond ((null? xs) '())                    ;if xs is empty, finish the recursion
          (else (cons (fn (head xs)) (map fn (tail xs)))))))

(define filter
  (lambda (pred xs)
    (cond ((null? xs) '())    ;if we reach the end of the list, end the recursion
          ((pred (head xs))   ;if true, keep the head and process the rest of the list 
           (cons (head xs) (filter pred (tail xs))))
          (else (filter pred (tail xs)))))) ;discard the head and process the rest

