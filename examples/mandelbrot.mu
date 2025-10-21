;; mandelbrot.mu — classic escape-time, MicroLisp core forms only

(define iters 200)
;(define xmin -2)
;(define xmax 0.5)
;(define ymin -1.3)
;(define ymax 1.3))
(define xmin -0.799)  
(define xmax -0.786)
(define ymin -0.1652)
(define ymax -0.1558)
(define height 2400)
(define width 3200)
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


;; helpers
(define clamp
  (lambda (x lo hi)
    (cond ((< x lo) lo)
          ((> x hi) hi)
          (else x))))

(define lerp
  (lambda (a b t)
    (+ a (* (- b a) t))))

;; black -> red -> orange -> yellow -> green -> blue -> white
(define rainbow-ish
  (lambda (val)
    (lets ((v  (- 1 val))
           (seg (/ 1.0 6.0))
           (idx (floor (/ v seg)))              ; 0..6 (6 only when v=1)
           (i (cond ((> idx 5) 5) (else idx)))   ; clamp index to 0..5
           (t (/ (- v (* i seg)) seg))           ; 0..1 position within segment

           ;; endpoints for each segment i
           ;; 0: black->red, 1: red->orange, 2: orange->yellow,
           ;; 3: yellow->green, 4: green->blue, 5: blue->white
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

           ;; linear 0..1 per channel within the segment
           (r_lin (lerp r0 r1 t))
           (g_lin (lerp g0 g1 t))
           (b_lin (lerp b0 b1 t))

           ;; apply gamma like your greyscale: x^(1/gamma), then to 0..255
           (r (floor (* 255 (^ r_lin (/ 1 gamma)))))
           (g (floor (* 255 (^ g_lin (/ 1 gamma)))))
           (b (floor (* 255 (^ b_lin (/ 1 gamma))))))
      (make-color r g b))))

(define color (lambda (its) (greyscale (rescale its))))


;this is for simulating the mandelbrot
(define cmplx (lambda (re im) (+ re (* im 0+i))))

(define mandel-point 
  (lambda (c)
    (let loop ((z 0) (its 0))
      (cond ((> (complex-magnitude z) 2) its)
            ((< its iters) (loop (+ c (^ z 2)) (+ its 1)))
            (else its)))))

(define rescale-dec
  (lambda (min max g)
    (cond ((< g 1) (error "G must be ≥ 1"))
          (else (lambda (c) (to-inexact (+ min (* (/ c g) (- max min)))))))))

(define xr (rescale-dec xmin xmax width))
(define yr (rescale-dec ymin ymax height))


(define canvas (create-graphics-device width height))
(define window (create-window canvas "mandelbrot"))

(define f (make-file "./images/mandelbrot19.png"))

(define main 
  (lambda (s)
    (do 
      (let loopx ((x 0))
        (cond ((> x (- width 1))#t)
              (else (do 
                      (let loopy ((y 0))
                        (cond ((> y (- height 1)) #t)
                              (else (do 
                                      (draw-pixel canvas x y (color (mandel-point (cmplx (xr x) (yr y)))))
                                      (loopy (+ y 1))))))
                    (print x) 
                    (refresh-window window)
                    (loopx (+ x 1))))))
      (write-image canvas f)
      (print "done"))))

