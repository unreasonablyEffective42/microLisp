(import lists)
(define black (make-color 0 0 0))
(define white (make-color 255 255 255))
(define red   (make-color 255 0 0))
(define blue  (make-color 0 0 255))

(define width  900)
(define height 600)

(define xmin -1)
(define ymin -1)
(define xmax 8)
(define ymax 5)

(define tx 10)

(define rescale-dec
  (lambda (min max g)
    (cond ((< g 1) (error "G must be ≥ 1"))
          (else (lambda (c) (to-inexact (+ min (* (/ c g) (- max min)))))))))

(define rescale-enc
  (lambda (min max g)
    (let ((spread (to-inexact (- max min))))
      (lambda (value)
        (to-inexact (* g (/ (- value min) spread)))))))

(define xr (rescale-dec xmin xmax width))
(define yr (rescale-dec ymax ymin height))
(define px (rescale-enc xmin xmax width))
(define py (rescale-enc ymax ymin height))

(define tickx 
  (lambda (ctx x) 
      (lines ctx 
             (floor (px x)) (floor (- (py 0) tx))
             (floor (px x)) (floor (+ (py 0) tx))
             black))))

(define ticky 
  (lambda (ctx y)
    (lets ((len (/ width 35)))
      (lines ctx 
             (floor (- (px 0) tx)) (floor (py y))
             (floor (+ (px 0) tx)) (floor (py y))
             black))))

(define axes 
  (lambda (canvas)
    (do 
      (lines canvas 
             (floor (px xmin)) (floor (py 0)) 
             (floor (px xmax)) (floor (py 0)) 
             black)
      (lines canvas
             (floor (px 0)) (floor (py ymin))
             (floor (px 0)) (floor (py ymax)) 
             black)
      (map (lambda (x) (tickx canvas x)) (lcomp xmin (+ xmax 1)))
      (map (lambda (y) (ticky canvas y)) (lcomp ymin (+ ymax 1)))
      (lets ((ctx (text-begin canvas))
             (numx (lambda (x)
               (do 
                  (text-set-font ctx "SansSerif" 'normal 10)
                  (text-set-color ctx 0 0 0 255)
                  (text-draw ctx (floor (- (px x) 2)) (floor (+ (py 0) 20)) (string x)))))
             (numy (lambda (y)
               (do 
                  (text-set-font ctx "SansSerif" 'normal 10)
                  (text-set-color ctx 0 0 0 255)
                  (text-draw ctx (floor (- (px 0) 20)) (floor (py y)) (string y))))))
        (do 
          (map numx (lcomp xmin xmax))
          (map numy (lcomp ymin ymax)))))))

(define scatter 
  (lambda (canvas xs color)
    (map (lambda (c) (fill-circle canvas (c 0)  (c 1) 4 color)) xs)))

(define explicit-plot
    (lambda (canvas fn color)
      (lets ((x-step 0.05)
             (xs   (interp xmin xmax x-step))
             (ys   (map (lambda (x) (floor (py (fn x)))) xs))
             (cs   (zip (map (lambda (x) (floor (px x))) xs) ys))
             (csp  (tail cs))
             (pairs (zip cs csp))
             (segments (map (lambda (p)
                              (append (head p) (head (tail p))))
                            pairs))
             (draw-seg (lambda (seg)
                         (let ((x0 (list-ref seg 0))
                               (y0 (list-ref seg 1))
                               (x1 (list-ref seg 2))
                               (y1 (list-ref seg 3)))
                           (lines canvas x0 y0 x1 y1 color)))))
        (map draw-seg segments))))

(define implicit-plot 
  (lambda (canvas fn color)
    (lets ((t-step 0.1)
           (coords (map fn (interp 0 1 0.01)))
           (xs (map (lambda (c) (floor (px (list-ref c 0)))) coords))
           (ys (map (lambda (c) (floor (py (list-ref c 1)))) coords))
           (cs (zip xs ys))
           (csp (tail cs))
           (pairs (zip cs csp))
           (segments (map (lambda (p)
                            (append (head p) (head (tail p))))
                          pairs))
           (draw-seg (lambda (seg)
                       (let ((x0 (list-ref seg 0))
                             (y0 (list-ref seg 1))
                             (x1 (list-ref seg 2))
                             (y1 (list-ref seg 3)))
                         (lines canvas x0 y0 x1 y1 color)))))
        (map draw-seg segments))))
     
    
(define graphing-demo 
  (lambda (s)
    (lets ((canvas (create-graphics-device width height))
           (window (create-window canvas "text"))
           (file (make-file "./images/graphing-demo.png"))
           (cx (map (lambda (c)
                      ($ (floor (px (c 0)))
                         (floor (py (c 1)))))
                      (list ($ 1 1) ($ 3 2) ($ 3 4) ($ 4 5) ($ 2 4) ($ 5.5 1)))))

      (do 
        (fill canvas white)
        (axes canvas)
        (scatter canvas cx red)
        (explicit-plot canvas (lambda (x) (^ x 0.7)) blue)
        (explicit-plot canvas (lambda (x)
                                (let ((shifted-x (- x 2))) ; translate curve 2 units right
                                  (+ (^ shifted-x 3)
                                     (* 0.22 (^ shifted-x 2))
                                     (* -4 shifted-x)
                                     1)))
                        (make-color 0 255 50))
        (implicit-plot canvas (lambda (t) 
                                (lets ((xc (rescale-dec 0 (* 2 3.1415926) 1))
                                       (yc (rescale-dec 0 (* 2 3.1415926) 1)))
                                  (lets ((x (+ 2 (cos (xc t))))
                                         (y (+ 2 (sin (yc t)))))
                                    (list x y))))
                       (make-color 255 0 255))

        (implicit-plot canvas (lambda (t) 
                                (lets ((xc (rescale-dec 0 (* 2 3.243) 1))
                                       (yc (rescale-dec 0 (* 2 3.243) 1)))
                                  (lets ((x (+ 4 (* 1.5 (cos (xc t)))))
                                         (y (+ 3 (* 1.5 (sin (yc t))))))
                                    (list x y))))
                       (make-color 255 150 0))



        (refresh-window window)
        (write-image canvas file)
        (print "done")))))
