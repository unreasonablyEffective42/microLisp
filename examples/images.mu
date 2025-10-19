(define red (make-color 255 0 0))
(define green (make-color 0 255 0))
(define blue (make-color 0 0 255))

(define big-pixel 
  (lambda (image x y c)
    (let loop-i ((i (- x 1)))
      (do 
        (cond ((eq? i (+ x 2)) '())
              (else (do
                      (let loop-j ((j (- y 1)))
                              (cond ((eq? j (+ y 2) '()))
                                    (else (do (draw-pixel image i j c)
                                              (loop-j (+ j 1))))))
                      (loop-i (+ i 1)))))))))

(define checkerboard
  (lambda (image c)
    (let loop-x ((x 0))
       (cond ((> x 100) '())
             (else (do 
                     (let loop-y ((y 0) (alt 0))
                       (cond ((> y 100) '())
                             (else (cond ((even? x)
                                          (cond ((eq? alt 1) (do (draw-pixel image x y c) (loop-y (+ y 1) 0)))
                                                (else (loop-y (+ y 1) 1))))
                                         ((odd? x)
                                          (cond ((eq? alt 0) (do (draw-pixel image x y c) (loop-y (+ y 1) 1)))
                                                (else (loop-y (+ y 1) 0))))
                                         (else (print "idk how you got here"))))))
                     (loop-x (+ x 1))))))))

(define big-checkerboard
  (lambda (image c)
    (let loop-x ((x 1) (a 0))
       (cond ((> x 100) '())
             (else (do 
                     (let loop-y ((y 1) (alt 0))
                       (cond ((> y 100) '())
                             (else (cond ((even? a)
                                          (cond ((eq? alt 1) (do (big-pixel image (* x 2) (* y 2) c) (loop-y (+ y 2) 0)))
                                                (else (loop-y (+ y 2) 1))))
                                         ((odd? a)
                                          (cond ((eq? alt 0) (do (big-pixel image (* x 2) (* y 2) c) (loop-y (+ y 2) 1)))
                                                (else (loop-y (+ y 2) 0))))
                                         (else (print "idk how you got here"))))))

                     (loop-x (+ x 2) (+ a 1))))))))

(define main
  (lambda (s)
    (lets ((f (make-file "./images/example.png"))
           (img (create-graphics-device 202 202))
           (window (create-window img "test")))
      (do 
        (big-checkerboard img blue)
        (let loop ((c 1))
          (cond ((> c 100) '())
                (else (do 
                        (big-pixel img (* c 2) (* c 2) red)
                        (loop (+ c 2))))))

        (let loop ((c 1))
          (cond ((> c 100) '())
                (else (do 
                        (big-pixel img (* c 2) (- 199 (* c 2)) green)
                        (loop (+ c 2))))))
        (refresh-window window)
        (write-image img f)))))

