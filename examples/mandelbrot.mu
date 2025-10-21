;; mandelbrot.mu — classic escape-time, MicroLisp core forms only

(define iters 100)
;(define xmin -2)
;(define xmax 0.5)
;(define ymin -1.3)
;(define ymax 1.3))
(define xmin 0.142)  
(define xmax 0.282)
(define ymin -0.605)
(define ymax -0.499))
(define height 600)
(define width 800)
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

(define color (lambda (its) (black-red-yellow-white (rescale its))))


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

(define f (make-file "./images/mandelbrot13.png"))

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
                    (loopx (+ x 1))))))
      (refresh-window window)
      (write-image canvas f)
      (print "done"))))

