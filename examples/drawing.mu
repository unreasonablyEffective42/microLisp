(define abs
  (lambda (n)
    (cond ((< n 0) (- 0 n))
          (else n))))

(define sgn
  (lambda (n)
    (cond ((> n 0) 1)
          ((< n 0) -1)
          (else 0))))

(define lerp
  (lambda (a b t)
    (+ a (* (- b a) t))))

(define red    (make-color 255 0 0))
(define blue   (make-color 0 0 255))
(define white  (make-color 255 255 255))

(define main 
  (lambda (s)
    (lets ((canvas (create-graphics-device 500 500))
           (window (create-window canvas "Sample")))
      (do 
        (let loop ((t 0))
            (let ((x1 (lerp 100 400 t))
                (y1 (lerp 100 300 t))
                (x2 (lerp 450 50  t))
                (y2 (lerp 450 50  t)))
              (cond ((> t 1) #t)
                    (else (do
                            (fill canvas white)
                            (circle canvas (floor x1) (floor y1) 30 red)
                            (circle canvas (floor x2) (floor y2) 20 blue)
                            (refresh-window window) 
                            (wait 10)
                            (loop (+ t 0.01)))))))
        (let loop ((t 0))
            (let ((x1 (lerp 400 300 t))
                  (y1 (lerp 300 100 t))
                  (x2 (lerp 50 50   t))
                  (y2 (lerp 50 450  t)))
              (cond ((> t 1) #t)
                    (else (do
                            (fill canvas white)
                            (circle canvas (floor x1) (floor y1) 30 red)
                            (circle canvas (floor x2) (floor y2) 20 blue)
                            (refresh-window window) 
                            (wait 10)
                            (loop (+ t 0.01)))))))

        (close-window window)))))
