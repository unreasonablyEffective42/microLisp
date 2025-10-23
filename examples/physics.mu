(define red    (make-color 255 0 0))
(define blue   (make-color 0 0 255))
(define black  (make-color 0 0 0))
(define white  (make-color 255 255 255))
(define yellow (make-color 255 255 0))
(define width 750)
(define height 750)
(define xmin 0)
(define xmax 40)
(define ymin 0)
(define ymax 40)

(define g -9.81)

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

(define rescale-dec
  (lambda (min max g)
    (cond ((< g 1) (do (print "G must be ≥ 1") #f))
          (else (lambda (c) (to-inexact (+ min (* (/ c g) (- max min)))))))))

(define rescale-pix
  (lambda (min max g)
    (cond ((< g 1) (do (print "G must be ≥ 1") #f))
          (else (lambda (c) (floor (to-inexact (+ min (* (/ c g) (- max min))))))))))

;convert pixel coords to math coords
(define xr (rescale-dec xmin xmax width))
(define yr (rescale-dec ymin ymax height))
;convert math coords to pixel coords
(define xp (rescale-pix 0 width (- xmax xmin)))
(define yp (rescale-pix height 0 (- ymax ymin)))

(define make-ball
   (lambda (r x y vx vy m c) 
     (lambda (msg)
       (lets ((v (^ (+ (^ vx 2) (^ vy 2)) 0.5)) ;velocity magnitude (m/s) 
              (u (* m g y))                     ;potential energy   (J)
              (k (* 0.5 m (^ v 2))))            ;kinetic energy     (J)
         (cond ((eq? msg 'coords) ($ x y))
               ((eq? msg 'x)         x)
               ((eq? msg 'y)         y)
               ((eq? msg 'radius)    r)
               ((eq? msg 'vx)        vx)
               ((eq? msg 'vy)        vy)
               ((eq? msg 'velocity)  v)
               ((eq? msg 'potential) u)
               ((eq? msg 'kinetic)   k)
               ((eq? msg 'color)     c)
               ((eq? msg 'update)
                 (lambda (nx ny vx vy)
                   (make-ball r nx ny vx vy m c)))
               (else (do (print "error invalid message to ball") #f)))))))

(define make-wall
  (lambda (x0 y0 x1 y1 c)
    (lambda (msg)
      (cond ((eq? msg 'x0)   x0)
            ((eq? msg 'x1)   x1)
            ((eq? msg 'y0)   y0)
            ((eq? msg 'y1)   y1)
            ((eq? msg 'color) c)
            (else (do (print "error invalid message to wall") #f))))))

(define vector-magnitude
  (lambda (vec)
    (^ (+ (^ (vec 0) 2) (^ (vec 1) 2)) 0.5)))

(define overlap?
  (lambda (ball1 ball2)
    (lets ((r1 (ball1 'radius))
           (r2 (ball2 'radius))
           (dr (+ r1 r2))
           (dx (vector-magnitude (- (ball1 'coords) (ball2 'coords)))))
      (cond ((> dx dr) #f)
            (else #t)))))

(define boundary-collision?
  (lambda (ball wall) #f))
    

(define display-wall
  (lambda (img wall)
    (lines img (xp (wall 'x0)) (yp (wall 'y0)) (xp (wall 'x1)) (yp (wall 'y1)) (wall 'color))))

(define display-ball 
  (lambda (img ball)
    (let ((cx    (xp (ball 'x)))
          (cy    (yp (ball 'y)))
          (r     (xp (ball 'radius)))
          (color (ball 'color)))
      (circle img cx cy r color))))

(define gravitational-acceleration 
  (lambda (ball t) 
    (lets ((x0  (ball 'x))
           (y0  (ball 'y))
           (vx0 (ball 'vx))
           (vy0 (ball 'vy)) 
           (x1  (+ (* vx0 t) x0))
           (y1  (+ (* 0.5 g (^ t 2)) (* vy0 t) y0))
           (vy1 (+ (* g t) vy0)))
      ((ball 'update) x1 y1 vx0 vy1))))
        

;r x y vx vy m c
(define main
  (lambda (s)
    (lets ((canvas (create-graphics-device width height))
           (window (create-window canvas "Physics Sim"))
           (fl (make-wall 0  1  40 1 black))
           (b1 (make-ball 2  20 35 0 0 0 red))
           (b2 (make-ball 3  10 10 0 0 0 blue))
           (b3 (make-ball 4  10 20 0 0 0 black))
           (b4 (make-ball 3  25 10 0 0 0 yellow)))
      (let loop ((t 0) (b1 b1))
        (cond ((> t 1) (do (print "done") (close-window window) #t))
              (else (do 
                      (fill canvas white)
                      (display-ball canvas b1)
                      (refresh-window window)
                      (wait 5)
                      (loop (+ t 0.0001) (gravitational-acceleration b1 t)))))))))






