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

;coordinates are tuples
(define index-coord
  (lambda (board coord)
    (let ((x (coord 0)) (y (coord 1)))
      (cond ((< x 0) 0)
            ((< y 0) 0)
            ((> x (-(size board) 1)) 0)
            ((> y (- (size board) 1)) 0)
            (else ((board y) x))))))

;Calculate the number of living neighbors to an x,y coordinate
(define alive-neighbors 
  (lambda (board coord)
    (lets ((x (coord 0)) 
           (y (coord 1))
           (coords (list (:: (- x 1) (- y 1)) (:: x (- y 1)) (:: (+ x 1) (- y 1))
                         (:: (- x 1) y)                      (:: (+ x 1) y)
                         (:: (- x 1) (+ y 1)) (:: x (+ y 1)) (:: (+ x 1) (+ y 1)))))
      (foldl + 0 (map (lambda (c) (index-coord board c)) coords)))))

;Determine if a cell is alive based on its state and its neighbors
(define new-value
  (lambda (board coord)
    (let ((neighbors (alive-neighbors board coord))
          (current (index-coord board coord)))
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
                            (cons (new-value board (:: x y)) xs))))) ; <-- PARENS WERE MISSING
               rows))))))))

;gui logic 
(define s (- (size board) 1))
(define black (make-color 0 0 0))
(define white (make-color 255 255 255))

;converts the (x,y) coords of a board position to the *15
;coordinates used by the display
(define resize
  (lambda (c)
    (:: (+ (* (c 0) 15) 7)
        (+ (* (c 1) 15) 7))))

;displays a cell at the board coordinates to the *15 coords
(define add-cell  
  (lambda (image coord color) 
    (lets ((resized (resize coord))
           (x (resized 0))
           (y (resized 1))) 
          (let loop-i ((i (- x 7))) 
              (cond ((eq? i (+ x 8)) '())
                    (else (do
                            (let loop-j ((j (- y 7)))
                              (cond ((eq? j (+ y 8) '()))
                                    (else (do 
                                            (draw-pixel image i j color)
                                            (loop-j (+ j 1))))))
                            (loop-i (+ i 1)))))))))

;renders the board to the graphics device
(define render-board
  (lambda (image board)
    (let loop-y ((y 0))
      (cond ((eq? y s) '())
            (else (do
                    (let loop-x ((x 0))
                      (cond ((eq? x s) '())
                            ((eq? (index-coord board (:: x y)) 1) 
                             (do (add-cell image (:: x y) black) 
                                 (loop-x (+ x 1))))
                            (else (do (add-cell image (:: x y) white) (loop-x (+ x 1))))))
                    (loop-y (+ y 1))))))))

(define main 
  (lambda (s)
    (lets ((canvas (create-graphics-device 400 400)) 
           (window (create-window canvas "GOL")))
      (let loop ((x 0) (board board))
        (do
          (render-board canvas board)
          (refresh-window window)
          (wait 1)
          (cond ((eq? x 50) (print "done"))
                (else (loop (+ x 1) (update board))))
          (close-window window))))))
;Other boards 

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

