;constants
(import lists)

(define iters 50)
;(define xmin -2)
;(define xmax 0.5)
;(define ymin -1.3)
;(define ymax 1.3))


;(define xmin -0.799)  
;(define xmax -0.7925)
;(define ymin -0.1652)
;(define ymax -0.1558)


; aspect = W/H, base_half_height = 1.0 if your initial y-range is [-1, 1]
(define mandelbrot-bounds
  (lambda (cx cy zoom aspect base_half_height)
    (lets ((hh (/ base_half_height zoom))
           (hw (* aspect hh)))
      ($ (- cx hw) (+ cx hw) (- cy hh) (+ cy hh)))))

(define ratio 16/9)
(define cx -0.7435669)
(define cy 0.1314023))
(define bounds (mandelbrot-bounds cx cy 1344.9 ratio 1.96875))
(define xmin (bounds 0))
(define xmax (bounds 1))
(define ymin (bounds 2))
(define ymax (bounds 3))
(define height (* 640 9))
(define width (* 640 16))
(define gamma 2.2)

;these are for making colors
(define rescale 
  (lambda (its)
    (cond ((> its iters) 1.0)
          (else (to-inexact (/ its iters))))))
 
(define greyscale 
  (lambda (val) 
    (let ((sat (floor (* 255 (^ val (/ 1 gamma))))))
      (make-color (- 255 sat) (- 255 sat) (- 255 sat)))))

(define black-red-yellow
  (lambda (val)
    (lets ((val (- 1 val))
           (r-lin (cond ((< val 0.5) (* 2 val)) (else 1)))
           (g-lin (cond ((< val 0.5) 0) (else (- (* 2 val) 1))))
           (r (floor (* 255 (^ r-lin (/ 1 gamma)))))
           (g (floor (* 255 (^ g-lin (/ 1 gamma))))))
      (make-color r g 0))))

(define black-red-yellow-white
  (lambda (val)
    (lets ((val (- 1 val))
           (r-lin (cond ((< val 0.3) (/ val 0.3))
                        ((< val 0.6) 1.0)
                        (else 1.0)))
           (g-lin (cond ((< val 0.3) 0.0)
                        ((< val 0.6) (/ (- val 0.3) 0.3))
                        (else 1.0)))
           (b-lin (cond ((< val 0.6) 0.0)
                        (else (/ (- val 0.6) 0.4))))
           (r (floor (* 255 (^ r-lin (/ 1 gamma)))))
           (g (floor (* 255 (^ g-lin (/ 1 gamma)))))
           (b (floor (* 255 (^ b-lin (/ 1 gamma))))))
      (make-color r g b))))

(define clamp
  (lambda (x lo hi)
    (cond ((< x lo) lo)
          ((> x hi) hi)
          (else x))))

(define lerp
  (lambda (a b t)
    (+ a (* (- b a) t))))

; black -> red -> orange -> yellow -> green -> blue -> white
(define rainbow-ish
  (lambda (val)
    (lets ((v  (- 1 val))
           (seg (/ 1.0 6.0))
           (idx (floor (/ v seg)))              ; 0..6 (6 only when v=1)
           (i (cond ((> idx 5) 5) (else idx)))   ; clamp index to 0..5
           (t (/ (- v (* i seg)) seg))           ; 0..1 position within segment

           ; endpoints for each segment i
           ; 0: black->red, 1: red->orange, 2: orange->yellow,
           ; 3: yellow->green, 4: green->blue2 5: blue->white
           (r0 (cond ((eq? i 0) 0.0) ((eq? i 1) 1.0) ((eq? i 2) 1.0)
                     ((eq? i 3) 1.0) ((eq? i 4) 0.0) (else 0.0)))
           (g0 (cond ((eq? i 0) 0.0) ((eq? i 1) 0.0) ((eq? i 2) 0.5)
                     ((eq? i 3) 1.0) ((eq? i 4) 1.0) (else 0.0)))
           (b0 (cond ((eq? i 0) 0.0) ((eq? i 1) 0.0) ((eq? i 2) 0.0)
                     ((eq? i 3) 0.0) ((eq? i 4) 0.0) (else 1.0)))

           (r1 (cond ((eq? i 0) 1.0) ((eq? i 1) 1.0) ((eq? i 2) 1.0)
                     ((eq? i 3) 0.0) ((eq? i 4) 0.0) (else 1.0)))
           (g1 (cond ((eq? i 0) 0.0) ((eq? i 1) 0.5) ((eq? i 2) 1.0)
                     ((eq? i 3) 1.0) ((eq? i 4) 0.0) (else 1.0)))
           (b1 (cond ((eq? i 0) 0.0) ((eq? i 1) 0.0) ((eq? i 2) 0.0)
                     ((eq? i 3) 0.0) ((eq? i 4) 1.0) (else 1.0)))

           (r_lin (lerp r0 r1 t))
           (g_lin (lerp g0 g1 t))
           (b_lin (lerp b0 b1 t))
 
           (r (floor (* 255 (^ r_lin (/ 1 gamma)))))
           (g (floor (* 255 (^ g_lin (/ 1 gamma)))))
           (b (floor (* 255 (^ b_lin (/ 1 gamma))))))
      (make-color r g b))))

(define color (lambda (its) (rainbow-ish (rescale its))))


;this is for simulating the mandelbrot
(define cmplx (lambda (re im) (+ re (* im 0+i))))

(define mandel-point 
  (lambda (c)
    (let loop ((z 0) (its 0))
      (cond ((> (complex-magnitude z) 2) its)
            ((< its iters) (loop (+ c (^ z 2)) (+ its 1)))
            (else its)))))

(define rescale-enc
  (lambda (min max g)
    (let ((spread (to-inexact (- max min))))
      (lambda (value)
        (to-inexact (* g (/ (- value min) spread)))))))

(define rescale-dec
  (lambda (min max g)
    (cond ((< g 1) (error "G must be ≥ 1"))
          (else (lambda (c) (to-inexact (+ min (* (/ c g) (- max min)))))))))

(define xr (rescale-dec xmin xmax width))
(define yr (rescale-dec ymin ymax height))
(define px (rescale-enc xmin xmax width))
(define py (rescale-enc ymin ymax height))

(define canvas (create-graphics-device width height))
(define window (create-window canvas "mandelbrot"))

(define f (make-file "./images/mandelbrot27.png"))

(define main 
  (lambda (s)
    (do 
      (let loopy ((y 0))
        (cond ((> y (- height 1)) #t)
              (else (do
                      (let fillx ((x 0) (row '()))
                        (cond ((> x (- width 1))
                               (set-row canvas y (list->vector (reverse row))))
                              (else (fillx (+ x 1)
                                           (cons (color (mandel-point (cmplx (xr x) (yr y)))) row)))))
                      (print y)
                      (cond ((eq? (% y 16) 0) (refresh-window window))
                            (else "#t"))
                      (loopy (+ y 1))))))
      (refresh-window window)
      (write-image canvas f)
      (print "done"))))
