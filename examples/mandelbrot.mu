;; mandelbrot.mu — classic escape-time, MicroLisp core forms only

(define iters 200)
;    (define xmin -2)
;    (define xmax 0.5)
;    (define ymin -1.3)
;    (define ymax 1.3))
(define xmin 0.142)  
(define xmax 0.282)
(define ymin -0.605)
(define ymax -0.499))
(define height 600)
(define width 800)

(define cmplx (lambda (re im) (+ re (* im 0+i))))

(define greyscale (lambda (sat) (make-color sat sat sat)))

(define blue-to-red
  (lambda (k)
    (lets ((kk (cond ((< k 0) 0) ((> k iters) iters) (else k)))
           (t (/ (* 1.0 kk) iters))
           (r (floor (* 255 t)))
           (g 0)
           (b (floor (* 255 (- 1 t)))))
      (make-color r g b))))

(define rescale 
  (lambda (its)
    (cond ((> its iters) 255)
          (else (floor (/ (+ (* 255 (- its 1))(/ (- iters 1) 2)) iters))))))

(define color (lambda (its) (greyscale (rescale its))))

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

(define f (make-file "./images/mandelbrot7.png"))

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

