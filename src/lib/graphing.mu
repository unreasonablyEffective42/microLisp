(import lists)

; Basic colors shared by plots
(define black (make-color 0 0 0))
(define white (make-color 255 255 255))
(define red   (make-color 255 0 0))
(define blue  (make-color 0 0 255))

; Plot configuration helpers (no more fixed globals)
(define make-plot-config
  (lambda (width height xmin xmax ymin ymax)
    ; fields: width height xmin xmax ymin ymax tick-len bg-color axis-color explicit-step param-step scatter-radius
    (list width height xmin xmax ymin ymax 10 white black 0.05 0.01 4)))

(define default-plot-config (make-plot-config 900 600 -1 8 -1 5))

(define cfg-width (lambda (cfg) (list-ref cfg 0)))
(define cfg-height (lambda (cfg) (list-ref cfg 1)))
(define cfg-xmin (lambda (cfg) (list-ref cfg 2)))
(define cfg-xmax (lambda (cfg) (list-ref cfg 3)))
(define cfg-ymin (lambda (cfg) (list-ref cfg 4)))
(define cfg-ymax (lambda (cfg) (list-ref cfg 5)))
(define cfg-tick (lambda (cfg) (list-ref cfg 6)))
(define cfg-bg (lambda (cfg) (list-ref cfg 7)))
(define cfg-axis-color (lambda (cfg) (list-ref cfg 8)))
(define cfg-explicit-step (lambda (cfg) (list-ref cfg 9)))
(define cfg-param-step (lambda (cfg) (list-ref cfg 10)))
(define cfg-scatter-radius (lambda (cfg) (list-ref cfg 11)))

(define rescale-dec
  (lambda (min max g)
    (cond ((< g 1) (error "G must be ≥ 1"))
          (else (lambda (c) (to-inexact (+ min (* (/ c g) (- max min)))))))))

(define rescale-enc
  (lambda (min max g)
    (let ((spread (to-inexact (- max min))))
      (lambda (value)
        (to-inexact (* g (/ (- value min) spread)))))))

(define make-px
  (lambda (cfg)
    (rescale-enc (cfg-xmin cfg) (cfg-xmax cfg) (cfg-width cfg))))

(define make-py
  (lambda (cfg)
    (rescale-enc (cfg-ymax cfg) (cfg-ymin cfg) (cfg-height cfg))))

(define make-xr
  (lambda (cfg)
    (rescale-dec (cfg-xmin cfg) (cfg-xmax cfg) (cfg-width cfg))))

(define make-yr
  (lambda (cfg)
    (rescale-dec (cfg-ymax cfg) (cfg-ymin cfg) (cfg-height cfg))))

(define point-ref
  (lambda (pt idx)
    ; allow both lists and callable vectors/tuples produced by $
    (cond ((list? pt) (list-ref pt idx))
          (else (pt idx)))))

(define draw-axes 
  (lambda (canvas cfg)
    (lets ((px (make-px cfg))
           (py (make-py cfg))
           (xmin (cfg-xmin cfg))
           (xmax (cfg-xmax cfg))
           (ymin (cfg-ymin cfg))
           (ymax (cfg-ymax cfg))
           (tick (cfg-tick cfg))
           (axis-color (cfg-axis-color cfg)))
      (lets ((tickx 
              (lambda (x) 
                (lines canvas 
                       (floor (px x)) (floor (- (py 0) tick))
                       (floor (px x)) (floor (+ (py 0) tick))
                       axis-color)))
             (ticky 
              (lambda (y)
                (lines canvas 
                       (floor (- (px 0) tick)) (floor (py y))
                       (floor (+ (px 0) tick)) (floor (py y))
                       axis-color))))
        (do 
          (lines canvas 
                 (floor (px xmin)) (floor (py 0)) 
                 (floor (px xmax)) (floor (py 0)) 
                 axis-color)
          (lines canvas
                 (floor (px 0)) (floor (py ymin))
                 (floor (px 0)) (floor (py ymax)) 
                 axis-color)
          (map tickx (interp xmin (+ xmax 1) 1))
          (map ticky (interp ymin (+ ymax 1) 1))
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
              (map numx (interp xmin xmax 1))
              (map numy (interp ymin ymax 1)))))))))

(define scatter 
  (lambda (canvas cfg points color)
    (lets ((px (make-px cfg))
           (py (make-py cfg))
           (radius (cfg-scatter-radius cfg)))
      (map (lambda (pt)
             (fill-circle canvas 
                          (floor (px (point-ref pt 0)))
                          (floor (py (point-ref pt 1)))
                          radius
                          color))
           points))))

(define explicit-plot
    (lambda (canvas cfg fn color)
      (lets ((px (make-px cfg))
             (py (make-py cfg))
             (xmin (cfg-xmin cfg))
             (xmax (cfg-xmax cfg))
             (step (cfg-explicit-step cfg))
             (xs   (interp xmin xmax step))
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
  (lambda (canvas cfg fn color)
    (lets ((px (make-px cfg))
           (py (make-py cfg))
           (step (cfg-param-step cfg))
           (coords (map fn (interp 0 1 step)))
           (xs (map (lambda (c) (floor (px (point-ref c 0)))) coords))
           (ys (map (lambda (c) (floor (py (point-ref c 1)))) coords))
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
    (lets ((cfg default-plot-config)
           (canvas (create-graphics-device (cfg-width cfg) (cfg-height cfg)))
           (window (create-window canvas "text"))
           (file (make-file "./images/graphing-demo.png"))
           (points (list ($ 1 1) ($ 3 2) ($ 3 4) ($ 4 5) ($ 2 4) ($ 5.5 1))))

      (do 
        (fill canvas (cfg-bg cfg))
        (draw-axes canvas cfg)
        (scatter canvas cfg points red)
        (explicit-plot canvas cfg (lambda (x) (^ x 0.7)) blue)
        (explicit-plot canvas cfg (lambda (x)
                                    (let ((shifted-x (- x 2))) ; translate curve 2 units right
                                      (+ (^ shifted-x 3)
                                         (* 0.22 (^ shifted-x 2))
                                         (* -4 shifted-x)
                                         1)))
                        (make-color 0 255 50))
        (implicit-plot canvas cfg (lambda (t) 
                                    (lets ((xc (rescale-dec 0 (* 2 3.1415926) 1))
                                           (yc (rescale-dec 0 (* 2 3.1415926) 1)))
                                      (lets ((x (+ 2 (cos (xc t))))
                                             (y (+ 2 (sin (yc t)))))
                                        (list x y))))
                       (make-color 255 0 255))

        (implicit-plot canvas cfg (lambda (t) 
                                    (lets ((xc (rescale-dec 0 (* 2 3.243) 1))
                                           (yc (rescale-dec 0 (* 2 3.243) 1)))
                                      (lets ((x (+ 4 (* 1.5 (cos (xc t)))))
                                             (y (+ 3 (* 1.5 (sin (yc t))))))
                                        (list x y))))
                       (make-color 255 150 0))



        (refresh-window window)
        (write-image canvas file)
        (print "done")
        (close-window window)))))
