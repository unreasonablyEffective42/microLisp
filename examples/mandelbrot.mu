;; mandelbrot.mu — classic escape-time, MicroLisp core forms only

(define mandelbrot
  (lambda (image width height center-re center-im scale max-iter)
    (lets ((inv-scale (/ 1.0 scale))
           (re0 (- center-re (* 0.5 width inv-scale)))
           (im0 (+ center-im (* 0.5 height inv-scale)))) ; + because screen y grows down
      (let loop-y ((y 0))
        (cond
          ((or (> y height) (eq? y height)) 'done)
          (else (lets ((c-im (- im0 (* y inv-scale))))
                  (let loop-x ((x 0))
                    (cond ((or (> x width) (eq? x width)) (loop-y (+ y 1)))
                          (else (lets ((c-re (+ re0 (* x inv-scale))))
                                  (let iter ((zr 0.0) (zi 0.0) (i 0))
                                    (cond ((or (or (> i max-iter) (eq? i max-iter)))
                                                    (> (+ (* zr zr) (* zi zi)) 4.0)))
                           ;; choose color and draw
                           (cond
                             ((or (> i max-iter) (eq? i max-iter))
                              (draw-pixel image x y (color 0 0 0))) ; interior = black
                             (else
                               (lets ((t (/ i max-iter))
                                      (R (round (* 255.0 t)))
                                      (G (round (* 255.0 (- 1.0 t))))
                                      (B (round (* 255.0 (* 4.0 t (- 1.0 t))))))
                                 (draw-pixel image x y (color R G B)))))
                           (loop-x (+ x 1)))
                          (else
                            ;; z <- z^2 + c
                            (lets ((zr2 (* zr zr))
                                   (zi2 (* zi zi))
                                   (twozrzi (* 2.0 zr zi))
                                   (new-zr (+ (- zr2 zi2) c-re))
                                   (new-zi (+ twozrzi c-im)))
                              (iter new-zr new-zi (+ i 1)))))))))))))))


(define W 1000)
(define H 1000)
(define cx -0.75)
(define cy 0.0)
(define scale 400.0)
(define iters 1000)

(define img (create-graphics-device 1000 1000))
(define window (create-window img "mandelbrot"))

(define main 
  (lambda (s)
    (mandelbrot img W H cx cy scale iters)
    (refresh-window window)
    (read)))
