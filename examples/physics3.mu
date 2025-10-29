(import lists)
(import strings)
(define red    (make-color 255 0 0))
(define blue   (make-color 0 0 255))
(define black  (make-color 0 0 0))
(define white  (make-color 255 255 255))
(define yellow (make-color 255 255 0))
(define purple (make-color 255 0 255))
(define green  (make-color 0 255 50))
(define orange (make-color 255 150 0))
(define width 750)
(define height 750)
(define xmin 0)
(define xmax 100)
(define ymin 0)
(define ymax 100)

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
   (lambda (r x y vx vy m c id) 
     (lambda (msg)
       (lets ((v (^ (+ (^ vx 2) (^ vy 2)) 0.5)) ;velocity magnitude (m/s) 
              (u (* m g y))                     ;potential energy   (J)
              (k (* 0.5 m (^ v 2)))             ;kinetic energy     (J)
              (p (* m v)))                      ;momentum           (kgm/s)
         (cond ((eq? msg 'coords) ($ x y))
               ((eq? msg 'v)      ($ vx vy))
               ((eq? msg 'x)         x)
               ((eq? msg 'y)         y)
               ((eq? msg 'radius)    r)
               ((eq? msg 'vx)        vx)
               ((eq? msg 'vy)        vy)
               ((eq? msg 'mass)      m)
               ((eq? msg 'momentum)  p)
               ((eq? msg 'velocity)  v)
               ((eq? msg 'potential) u)
               ((eq? msg 'kinetic)   k)
               ((eq? msg 'color)     c)
               ((eq? msg 'id)        id)
               ((eq? msg 'update)
                 (lambda (nx ny vx vy)
                   (make-ball r nx ny vx vy m c id)))
               (else (do (print "error invalid message to ball") #f)))))))

(define make-wall
  (lambda (x0 y0 x1 y1 c)
    (lambda (msg)
      (lets ((dx (- x1 x0))
             (dy (- y1 y0))
             (m  (^ (+ (^ dx 2) (^ dy 2)) 0.5))
             (n  ($ (/ dy m) (/ dx m))))
        (cond ((eq? msg 'x0)   x0)
               ((eq? msg 'x1)   x1)
               ((eq? msg 'y0)   y0)
               ((eq? msg 'y1)   y1)
               ((eq? msg 'color) c)
               ((eq? msg 'normal) n)
               (else (do (print "error invalid message to wall") #f)))))))

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
  (lambda (ball wall)
    (lets ((r  (ball 'radius))
           (x0 (ball 'x))
           (y0 (ball 'y))
           (x1 (wall 'x0))
           (y1 (wall 'y0))
           (x2 (wall 'x1))
           (y2 (wall 'y1))
           (m (abs (- (* (- x0 x1) (- y2 y1)) (* (- y0 y1) (- x2 x1)))))
           (n (^ (+ (^ (- x2 x1) 2) (^ (- y2 y1) 2)) 0.5))
           (d (/ m n)))
      (cond ((> d r) #f)
            (else #t)))))

(define distance 
  (lambda (x1 y1 x2 y2)
    (let ((x (^ (- x2 x1) 2))
          (y (^ (- y2 y1) 2)))
      (^ (+ x y) 0.5))))

(define ball-collision? 
  (lambda (ball1 ball2)
    (lets ((r (+ (ball1 'radius) (ball2 'radius)))
           (x1 (ball1 'x))
           (y1 (ball1 'y))
           (x2 (ball2 'x))
           (y2 (ball2 'y))
           (d  (distance x1 y1 x2 y2)))
      (cond ((> d r) #f)
            ((eq? (ball1 'id) (ball2 'id)) #f)
            (else #t)))))

(define collide 
  (lambda (ball1 ball2)
    (cond ((ball-collision? ball1 ball2)
            (lets ((m1  (ball1 'mass))
                   (m2  (ball2 'mass))
                   (x1  (ball1 'coords))
                   (x2  (ball2 'coords))
                   (v1  (ball1 'v))
                   (v2  (ball2 'v))
                   (k   (/ (* 2 m2) (+ m1 m2)))
                   (w   (/ (dot (vsub v1 v2) (vsub x1 x2)) (^ (vector-magnitude (vsub x1 x2)) 2)))
                   (vf (vsub v1 (smul (* k w) (vsub x1 x2)))))
              ((ball1 'update) (x1 0) (x1 1) (vf 0) (vf 1))))
          (else ball1))))



(define display-wall
  (lambda (img wall)
    (lines img (xp (wall 'x0)) (yp (wall 'y0)) (xp (wall 'x1)) (yp (wall 'y1)) (wall 'color))))

(define display-ball 
  (lambda (img ball)
    (let ((cx    (xp (ball 'x)))
          (cy    (yp (ball 'y)))
          (r     (xp (ball 'radius)))
          (color (ball 'color)))
    (do
      (circle img cx cy r color)
      (fill-circle img cx cy r color)))))

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

(define dot 
  (lambda (v1 v2) 
    (+ (* (v1 0) (v2 0)) (* (v1 1) (v2 1)))))

(define vadd 
  (lambda (v1 v2) 
    ($ (+ (v1 0) (v2 0)) (+ (v1 1) (v2 1)))))

(define vsub
  (lambda (v1 v2) 
    ($ (- (v1 0) (v2 0)) (- (v1 1) (v2 1)))))

(define smul
  (lambda (s v)
    ($ (* s (v 0)) (* s (v 1)))))

(define reflect 
  (lambda (v n)
    (lets ((vx0 (v 0))
           (vy0 (v 1))
           (nx  (n 0))
           (ny  (n 1))
           (vn  (dot v n))
           (vx  (- vx0 (* 2 vn nx)))
           (vy  (- vy0 (* 2 vn ny))))
      ($ vx vy))))

;currently cursed
;r x y vx vy m c id
(define main
  (lambda (s)
    (lets ((canvas (create-graphics-device width height))
           (window (create-window canvas "Physics Sim"))
           (walls (list  
             (make-wall 0  3  100 3 black)
             (make-wall 0  99 100 99 black)
             (make-wall 1  0  1   100 black)
             (make-wall 99 0  99  100 black)))
           (balls
             (flatten (map (lambda (x)
                        (map (lambda (y)
                               (cond ((even? x) (make-ball 1 (* x 5) (* y 5) 5 5 1 red (list->string `("ball" ,(string x) ,(string y)))))
                                     ((odd? x) (make-ball 1 (* x 5) (* y 5) -5 5 1 red (list->string `("ball" ,(string x) ,(string y)))))))
                             (lcomp 1 10)))
                      (lcomp 1 10))))
           (file (make-file "./video/Simulation4.mp4"))
           (nothing (start-recording canvas 120 file)))
      (let loop ((t 0) (ts 0.0083333333) (balls balls) (a 0))
        (cond ((> t 30) (do (stop-recording canvas) (print "done") (close-window window) #t))
              (else 
                (do 
                    (fill canvas white)
                    (map (lambda (wall) (display-wall canvas wall)) walls)
                    (map (lambda (ball) (display-ball canvas ball)) balls)
                    (encode-frame canvas)
                    (refresh-window window)
                    (loop (+ t ts) 
                          ts 
                          (map (lambda (ball) (gravitational-acceleration ball ts))                                           
                               (map (lambda (ball)
                                       (foldl (lambda (b wall)
                                                (cond ((boundary-collision? b wall)
                                                        (lets ((n (wall 'normal))
                                                               (v0 ($ (b 'vx) (b 'vy)))
                                                               (x  (b 'x))
                                                               (y  (b 'y))
                                                               (v1 (reflect v0 n)))
                                                               ((b 'update) x y (v1 0) (v1 1))))
                                                      (else b)))
                                              ball
                                              walls))
                                    (map (lambda (ball1)
                                           (foldl (lambda (acc b2) (collide acc b2))
                                                  ball1 
                                                  balls))
                                         balls)))
                            (cond ((eq? a 10) (do 
                                                 (clear) 
                                                 (print "Time: ") 
                                                 (print t) 
                                                 (map (lambda (ball) (do (printf (ball 'id)) (printf " energy ")(printf (- (ball 'kinetic) (ball 'potential)))(print "j")))
                                                      balls) 
                                                 (printf "Total Energy: ") 
                                                 (print (foldl (lambda (s ball) (+ s (- (ball 'kinetic) (ball 'potential)))) 0 balls))
                                                 0))
                                  (else (+ a 1)))))))))))
