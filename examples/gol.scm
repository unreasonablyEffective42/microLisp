;simulation logic
;(define board 
;  ($ ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
;     ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;     ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;     ($ 0 0 0 0 0 1 1 1 0 0 0 1 1 1 0 0 0 0 0 0)
;     ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;     ($ 0 0 0 1 0 0 0 0 1 0 1 0 0 0 0 1 0 0 0 0)
;     ($ 0 0 0 1 0 0 0 0 1 0 1 0 0 0 0 1 0 0 0 0)
;     ($ 0 0 0 1 0 0 0 0 1 0 1 0 0 0 0 1 0 0 0 0)
;     ($ 0 0 0 0 0 1 1 1 0 0 0 1 1 1 0 0 0 0 0 0)
;     ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;     ($ 0 0 0 0 0 1 1 1 0 0 0 1 1 1 0 0 0 0 0 0)
;     ($ 0 0 0 1 0 0 0 0 1 0 1 0 0 0 0 1 0 0 0 0)
;     ($ 0 0 0 1 0 0 0 0 1 0 1 0 0 0 0 1 0 0 0 0)
;     ($ 0 0 0 1 0 0 0 0 1 0 1 0 0 0 0 1 0 0 0 0)
;     ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;     ($ 0 0 0 0 0 1 1 1 0 0 0 1 1 1 0 0 0 0 0 0)
;     ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;     ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;     ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)))

;empty 25x25 board
;  (define board
;    ($ ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)
;       ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)))
;  R-pentomino on 25x25     
  (define board                                               
   ($ ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 1 1 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)  
      ($ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0)))





(define display
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
            ((> x (-(size m) 1)) 0)
            ((> y (- (size m) 1)) 0)
            (else ((m (c 1)) (c 0)))))))


(define alive-neighbors 
  (lambda (x y board)
    (let ((coords (list (:: (- x 1) (- y 1)) (:: x (- y 1)) (:: (+ x 1) (- y 1))
                        (:: (- x 1) y)                      (:: (+ x 1) y)
                        (:: (- x 1) (+ y 1)) (:: x (+ y 1)) (:: (+ x 1) (+ y 1)))))
      (foldl + 0 (map (lambda (c) (index-coord board c)) coords)))))

(define new-value
  (lambda (c board)
    (let ((neighbors (alive-neighbors (c 0) (c 1) board))
          (current (index-coord board c)))
      (cond ((and (eq? current 0) (eq? neighbors 3)) 1)
            ((and (eq? current 1) (or (eq? neighbors 2) (eq? neighbors 3))) 1)
            ((< neighbors 2) 0)
            (else 0)))))

(define update
  (lambda (board)
    (let ((s (size board))) 
      (let loop-y ((y 0) (rows '()))
        (cond
          ((eq? y s)
           (vector (reverse rows)))                 ; rows are already vectors
          (else
           (loop-y (+ y 1)
             (cons
               (let loop-x ((x 0) (xs '()))
                 (cond
                   ((eq? x s) (vector (reverse xs))) ; vector of numbers
                   (else
                    (loop-x (+ x 1)
                            (cons (new-value (:: x y) board) xs))))) ; <-- PARENS WERE MISSING
               rows))))))))
;gui logic 
(define s (- (size board) 1))
(define pixel-size 15)
(define raw-pixels (* s pixel-size))

(define black (make-color 0 0 0))
(define white (make-color 255 255 255))


(define resize
  (lambda (c)
    (:: (+ (* (c 0) 15) 7)
        (+ (* (c 1) 15) 7))))

(define add-cell  
  (lambda (image coord color) 
    (lets ((resized (resize coord))
           (x (resized 0))
           (y (resized 1))) 
          (let loop-i ((i (- x 7)))
            (do
              (cond ((eq? i (+ x 8)) '())
                    (else (do
                            (let loop-j ((j (- y 7)))
                              (cond ((eq? j (+ y 8) '()))
                                    (else (do 
                                            (draw-pixel image i j color)
                                            (loop-j (+ j 1))))))
                                (loop-i (+ i 1))))))))))

(define render-board
  (lambda (image board)
    (let loop-y ((y 0))
      (cond ((eq? y s) '())
            (else (do
                    (let loop-x ((x 0))
                      (cond ((eq? x s) '())
                            ((eq? (index-coord board (:: x y)) 1) (do (add-cell image (:: x y) black) (loop-x (+ x 1))))
                            (else (do (add-cell image (:: x y) white) (loop-x (+ x 1))))))
                    (loop-y (+ y 1))))))))
(define main 
  (lambda (s)
    (lets ((view (create-graphics-device 400 400))
           (window (create-window view "GOL")))
      (let loop ((x 0) (board board))
        (do
          (render-board view board)
          (refresh-window window)
          (wait 1)
          (cond ((eq? x 100) (print "done"))
                (else (loop (+ x 1) (update board)))))))))



