(import lists)
(import strings)

; Rectangular collision demo inspired by physics_refactored

(define concat-chars
  (lambda (a b)
    (let loop ((xs (reverse a)) (acc b))
      (cond ((null? xs) acc)
            (else (loop (tail xs) (cons (head xs) acc)))))))

(define string-cat2
  (lambda (a b)
    (list->string (concat-chars (string->list a) (string->list b)))))

(define sims-root "./simulations")
(define sim-folder "sim-rects-inelastic")
(define video-name "rect-collision-inelastic.mp4")

; masses from first run (grams -> kg for consistency)
(define m1 0.321)
(define m2 0.318)
(define cor 0.6012311) ; coefficient of restitution from second set

(define width-px 640) ; 20% narrower
(define height-px 300)
(define world-width 4.0)   ; meters
(define world-height 1.5)
(define scale (/ width-px world-width)) ; px per meter

(define dt 0.0083333333)
(define duration 12.0)
(define fps 120)
(define record? #t)

(define bg (make-color 255 255 255))
(define rect1-color (make-color 255 0 0))
(define rect2-color (make-color 0 0 255))
(define text-color (make-color 0 0 0))
(define black (make-color 0 0 0))

(define rect1-width 0.32) ; 20% narrower
(define rect1-height 0.2)
(define rect2-width 0.32) ; 20% narrower
(define rect2-height 0.2)

(define rect1-x0 0.5)  ; meters (left edge, adjusted for narrower view)
(define rect2-x0 1.9)  ; moved closer toward midpoint and narrower view
(define base-y 0.65)   ; meters (lower edge for both)

; initial velocities from first run (m/s)
(define rect1-v 0.258)
(define rect2-v 0.0)

(define path-join
  (lambda (base name)
    (list->string (concat-chars (string->list base) (string->list (string-cat2 "/" name))))))

(define path-basename
  (lambda (path)
    (let loop ((chars (reverse (string->list path))) (acc '()))
      (cond ((null? chars) (list->string acc))
            ((eq? (head chars) "/") (list->string acc))
            (else (loop (tail chars) (cons (head chars) acc)))))))

(define format-sim-time
  (lambda (time)
    (let ((ms (floor (+ 0.5 (* time 1000))))) ; round to nearest millisecond
      (let ((seconds (to-inexact (/ ms 1000))))
        (string-cat2 "t=" (string-cat2 (string seconds) " s"))))))

(define format-vel
  (lambda (v)
    (number->fixed-string v 4)))

(define ensure-directory
  (lambda (path)
    (make-directory path)))

(define to-px-x
  (lambda (x)
    (floor (* x scale))))

(define to-px-y
  (lambda (y)
    (floor (- height-px (* y scale)))))

(define draw-rect
  (lambda (canvas x0 y0 w h color)
    (let ((x1 (+ x0 w))
          (y1 (+ y0 h)))
      (do
        (lines canvas x0 y0 x1 y0 color)
        (lines canvas x1 y0 x1 y1 color)
        (lines canvas x1 y1 x0 y1 color)
        (lines canvas x0 y1 x0 y0 color)))))

(define draw-axis
  (lambda (canvas)
    (let ((axis-y (- height-px 60)) ; above velocity labels, below rectangles
          (tick 6)
          (xs (interp rect1-x0 world-width 0.5))
          (ctx (text-begin canvas)))
      (do
        (lines canvas 0 axis-y width-px axis-y black)
        (text-set-font ctx "SansSerif" 'normal 12)
        (text-set-color ctx 0 0 0 255)
        (map (lambda (x)
               (let ((px (to-px-x x)))
                 (do
                   (lines canvas px (- axis-y tick) px (+ axis-y tick) black)
                   (text-draw ctx (- px 8) (+ axis-y 18) (number->fixed-string (- x rect1-x0) 1)))))
             xs)
        (text-end ctx)))))

(define render-frame
  (lambda (canvas window time x1 x2 v1 v2)
    (do
      (fill canvas bg)
      (let ((ctx (text-begin canvas)))
        (do
          (text-set-font ctx "SansSerif" 'normal 16)
          (text-set-color ctx 0 0 0 255)
          (text-draw ctx 12 24 (format-sim-time time))
          (text-set-font ctx "SansSerif" 'normal 14)
          (text-draw ctx 12 (- height-px 20) (string-cat2 "v1=" (string-cat2 (format-vel v1) " m/s"))) ; lower-left
          (text-draw ctx (- width-px 140) (- height-px 20) (string-cat2 "v2=" (string-cat2 (format-vel v2) " m/s"))) ; lower-right
          (text-set-font ctx "SansSerif" 'normal 16)
          (text-draw ctx (- (/ width-px 2) 60) 40 (string-cat2 "COR: e ≈ " (string cor))) ; center-ish
          (text-end ctx)))
      (draw-rect canvas
                 (to-px-x x1) (to-px-y base-y)
                 (to-px-x rect1-width) (- (to-px-y 0) (to-px-y rect1-height)) rect1-color)
      (draw-rect canvas
                 (to-px-x x2) (to-px-y base-y)
                 (to-px-x rect2-width) (- (to-px-y 0) (to-px-y rect2-height)) rect2-color)
      (draw-axis canvas)
      (refresh-window window))))

(define collide-1d
  (lambda (m1 m2 u1 u2 e)
    (let ((v1 (/ (+ (* m1 u1) (* m2 u2) (* m2 e (- u2 u1))) (+ m1 m2)))
          (v2 (/ (+ (* m1 u1) (* m2 u2) (* m1 e (- u1 u2))) (+ m1 m2))))
      (list v1 v2))))

(define main
  (lambda (s)
    (lets ((canvas (create-graphics-device width-px height-px))
           (window (create-window canvas "Rect Collision"))
           (sim-path (path-join sims-root sim-folder))
           (video-path (path-join sim-path (path-basename video-name))))
      (do
        (ensure-directory sims-root)
        (ensure-directory sim-path)
        (start-recording canvas fps (make-file video-path))
        (let loop ((t 0.0)
                   (x1 rect1-x0)
                   (x2 rect2-x0)
                   (v1 rect1-v)
                   (v2 rect2-v)
                   (hit? #f))
          (cond ((>= t duration)
                 (do
                   (stop-recording canvas)
                   (close-window window)
                   '#t))
                (else
                  (lets ((next-x1 (+ x1 (* v1 dt)))
                         (next-x2 (+ x2 (* v2 dt)))
                         (will-hit? (cond (hit? #f)
                                          (else (>= (+ next-x1 rect1-width) next-x2))))
                         (nv1 (cond (will-hit? (list-ref (collide-1d m1 m2 v1 v2 cor) 0)) (else v1)))
                         (nv2 (cond (will-hit? (list-ref (collide-1d m1 m2 v1 v2 cor) 1)) (else v2))))
                    (do
                      (render-frame canvas window t x1 x2 v1 v2)
                      (encode-frame canvas)
                      (loop (+ t dt)
                            (cond (will-hit? (+ x1 (* nv1 dt))) (else next-x1))
                            (cond (will-hit? (+ x2 (* nv2 dt))) (else next-x2))
                            nv1
                            nv2
                            (or hit? will-hit?))))))))))))
