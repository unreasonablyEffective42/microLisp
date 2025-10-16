(define board 
  ($ ($ 0 0 0 0 0 0 0 0)
     ($ 0 0 1 0 0 0 0 0)
     ($ 0 0 0 1 0 0 0 0)    
     ($ 0 0 1 0 0 0 0 0)
     ($ 0 1 0 0 0 0 0 0)
     ($ 0 0 0 0 0 0 0 0)
     ($ 0 0 0 0 0 0 0 0)
     ($ 0 0 0 0 0 0 0 0)))

(define display-board 
  (lambda (board)
    (let loop-y ((y 0))
      (do
        (printf "|")
        (let loop-x ((x 0))
          (cond ((eq? x (- (size board) 1)) (do (printf ((board y) x)) '()))
                ((< x (- (size board) 2)) (do   
                                            (printf ((board y) x))
                                            (printf " ")
                                            (loop-x (+ x 1))))
                (else "idk how you got here")))
        (print "|")
        (cond ((eq? y (- (size board) 1) '()))
              (else (loop-y (+ y 1))))))))

;coordinates are tuples
(define index-coord
  (lambda (m c)
    (let ((x (c 0)) (y (c 1)))
      (cond ((< x 0) 0)
            ((< y 0) 0)
            ((> x (size m)) 0)
            ((> y (size m)) 0)
            (else ((m (c 1)) (c 0)))))))


(define alive-neighbors 
  (lambda (x y board)
    (let ((coords (list (:: (- x 1) (- y 1)) (:: x (- y 1)) (:: (+ x 1) (- y 1))
                        (:: (- x 1) y)                      (:: (+ x 1) y)
                        (:: (- x 1) (+ y 1)) (:: x (+ y 1)) (:: (+ x 1) (+ y 1)))))
      (foldl + 0 (map (lambda (c) (index-coord board c)) coords)))))


        
