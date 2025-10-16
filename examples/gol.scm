(define board 
  ($ ($ 0 0 0 0 0 0 0 0)
     ($ 0 0 0 0 0 0 0 0)
     ($ 0 0 0 0 0 0 0 0)    
     ($ 0 0 0 0 0 0 0 0)
     ($ 0 0 0 0 0 0 0 0)
     ($ 0 0 0 0 0 0 0 0)
     ($ 0 0 0 0 0 0 0 0)
     ($ 0 0 0 0 0 0 0 0)))

(define display-board 
  (lambda (board)
    (let loop-y ((y 0))
      (do
        (printf "|")
        (let loop-x ((x 0))
          (cond ((eq? x size) '())
                (else (do   
                        (printf ((board y) x))
                        (printf " ")
                        (loop-x (+ x 1))))))
        (print "|")
        (cond ((eq? y size) '())
              (else (loop-y (+ y 1))))))))


        
