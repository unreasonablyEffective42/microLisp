(import lists)
(import strings)

(define simulation-output-folder "sim-005")
(define simulations-root "./simulations")

(define concat-chars
  (lambda (a b)
    (let loop ((xs (reverse a)) (acc b))
      (cond ((null? xs) acc)
            (else (loop (tail xs) (cons (head xs) acc)))))))

(define string-cat2
  (lambda (a b)
    (list->string (concat-chars (string->list a) (string->list b)))))

(define path-join
  (lambda (base name)
    (string-cat2 base (string-cat2 "/" name))))

(define path-basename
  (lambda (path)
    (let loop ((chars (reverse (string->list path))) (acc '()))
      (cond ((null? chars) (list->string acc))
            ((eq? (head chars) "/") (list->string acc))
            (else (loop (tail chars) (cons (head chars) acc)))))))

(define ensure-directory
  (lambda (path)
    (make-directory path)))

(define join-with
  (lambda (values delim)
    (let loop ((xs values) (acc '()))
      (cond ((null? xs) (list->string acc))
            ((null? (tail xs))
             (loop (tail xs) (concat-chars acc (string->list (head xs)))))
            (else
              (loop (tail xs)
                    (concat-chars
                      (concat-chars acc (string->list (head xs)))
                      (string->list delim))))))))

(define format-row
  (lambda (values)
    (join-with (map (lambda (v) (string v)) values) ",")))

(define format-sim-time
  (lambda (time)
    (let ((ms (floor (+ 0.5 (* time 1000))))) ; round to nearest millisecond
      (let ((seconds (to-inexact (/ ms 1000))))
        (string-cat2 "t="
          (string-cat2 (string seconds) " s"))))))

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
(define top-offset 40) ; reserve space for title/labels
(define xmin 0)
(define xmax 100)
(define ymin 0)
(define ymax 100)

(define g -9.81)
(define e 0.95)

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
(define yp-raw (rescale-pix (- height top-offset) 0 (- ymax ymin)))
(define yp (lambda (y) (+ top-offset (yp-raw y))))

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
                   (k   (/ (* (+ 1 e) m2) (+ m1 m2)))
                   (w   (/ (dot (vsub v1 v2) (vsub x1 x2)) (^ (vector-magnitude (vsub x1 x2)) 2)))
                   (vf (vsub v1 (smul (* k w) (vsub x1 x2))))
                   (sum-r (+ (ball1 'radius) (ball2 'radius)))
                   (delta (vsub x1 x2))
                   (dist (vector-magnitude delta))
                   (penetration (- sum-r dist))
                   (normal (cond ((= dist 0) ($ 1 0))
                                 (else (smul (/ 1 dist) delta))))
                   (correction (/ penetration 2))
                   (pos-adjust (smul correction normal))
                   (x1-new (cond ((> penetration 0) (vadd x1 pos-adjust))
                                 (else x1))))

              ((ball1 'update) (x1-new 0) (x1-new 1) (vf 0) (vf 1))))
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
           (vx  (- vx0 (* (+ 1 e) vn nx)))
           (vy  (- vy0 (* (+ 1 e) vn ny))))
      ($ vx vy))))

; --------- simulation helpers ---------

(define make-scenario
  (lambda (title width height duration time-step fps output record? log-interval walls balls)
    (lambda (msg)
      (cond ((eq? msg 'title) title)
            ((eq? msg 'width) width)
            ((eq? msg 'height) height)
            ((eq? msg 'duration) duration)
            ((eq? msg 'time-step) time-step)
            ((eq? msg 'fps) fps)
            ((eq? msg 'output) output)
            ((eq? msg 'record?) record?)
            ((eq? msg 'log-interval) log-interval)
            ((eq? msg 'walls) walls)
            ((eq? msg 'balls) balls)
            (else (do (print "error invalid message to scenario") #f))))))

(define default-walls
  (list  (make-wall 0  3  100 3 black)
         (make-wall 0  99 100 99 black)
         (make-wall 1  0  1   100 black)
         (make-wall 99 0  99  100 black)))

(define default-balls
  (list
    (make-ball 2  10 55 3 25 1 red       'red___)
    (make-ball 3  10 10 9 35 2 blue      'blue__)
    (make-ball 4  10 20 5 10 3 black     'black_)
    (make-ball 5  50 70 12 3 7 green     'green_)
    (make-ball 6  60 40 -10 -10 9 orange 'orange)
    (make-ball 3  30 20 -5 -20 5 purple  'purple)))

(define default-scenario
  (make-scenario
    "Physics Sim (refactored)"
    width
    height
    20 
    0.0083333333
    120
    "./video/Simulation-refactored3.mp4"
    #t
    10
    default-walls
    default-balls))

(define energy
  (lambda (ball)
    (- (ball 'kinetic) (ball 'potential))))

(define total-energy
  (lambda (balls)
    (foldl (lambda (sum ball) (+ sum (energy ball))) 0 balls)))

(define log-energy
  (lambda (time balls)
    (do
      (clear)
      (print "Time: ")
      (print time)
      (map (lambda (ball)
             (do (printf (ball 'id))
                 (printf " speed ")
                 (printf (ball 'velocity))
                 (printf "m/s, KE ")
                 (printf (ball 'kinetic))
                 (printf "J, PE ")
                 (printf (ball 'potential))
                 (printf "J, Net ")
                 (printf (energy ball))
                 (print "J")))
           balls)
      (printf "Total Energy: ")
      (print (total-energy balls)))))

(define maybe-log-energy
  (lambda (interval step time balls)
    (cond ((or (eq? interval '#f) (<= interval 0)) '#t)
          ((eq? (% step interval) 0) (log-energy time balls))
          (else '#t))))

(define capture-ball-stats
  (lambda (time balls)
    (let loop ((xs balls) (rows '()))
      (cond ((null? xs) rows)
            (else
              (let ((ball (head xs)))
                (loop (tail xs)
                      (cons (list time
                                  (ball 'id)
                                  (ball 'velocity)
                                  (ball 'kinetic)
                                  (ball 'potential)
                                  (energy ball))
                            rows))))))))

(define capture-total-stats
  (lambda (time balls)
    (let loop ((xs balls) (ke 0) (pe 0) (net 0))
      (cond ((null? xs) (list time ke pe net))
            (else
              (let ((ball (head xs)))
                (loop (tail xs)
                      (+ ke (ball 'kinetic))
                      (+ pe (ball 'potential))
                      (+ net (energy ball)))))))))

(define flatten-ball-rows
  (lambda (rows acc)
    (foldl (lambda (acc row) (cons row acc)) acc rows)))

(define write-energy-csv
  (lambda (folder ball-rows total-rows)
    (let ((count-rows (lambda (xs)
                        (let loop ((xs xs) (n 0))
                          (cond ((null? xs) n)
                                (else (loop (tail xs) (+ n 1)))))))))
      (lets ((ball-strings (map (lambda (row)
                                  (format-row
                                    (list (list-ref row 0)
                                          (list-ref row 1)
                                          (list-ref row 2)
                                          (list-ref row 3)
                                          (list-ref row 4)
                                          (list-ref row 5))))
                                ball-rows))
             (total-strings (map (lambda (row)
                                   (format-row
                                     (list (list-ref row 0)
                                           (list-ref row 1)
                                           (list-ref row 2)
                                           (list-ref row 3))))
                                 total-rows))
             (rows (append
                     (append
                       (append (list "time,ball-id,speed,kinetic,potential,total")
                               ball-strings)
                       (list ""))
                     (append (list "time,total-kinetic,total-potential,total-energy")
                             total-strings)))
             (csv-file (make-file (path-join folder "energies.csv"))))
        (do
          (print "writing energy csv…")
          (print "csv rows: ")
          (print (count-rows rows))
          (let loop ((remaining rows) (written 0))
            (cond ((null? remaining) 'done)
                  ((= (% written 100) 0)
                   (do
                     (print "writing row ")
                     (print written)
                     (loop (tail remaining) (+ written 1))))
                  (else (loop (tail remaining) (+ written 1)))))
          (write-lines csv-file rows)
          (print "energy csv written."))))))

(define update-ball-with-wall
  (lambda (ball wall)
    (cond ((boundary-collision? ball wall)
            (lets ((n (wall 'normal))
                   (v0 ($ (ball 'vx) (ball 'vy)))
                   (pos ($ (ball 'x) (ball 'y)))
                   (v1 (reflect v0 n))
                   (wall-point ($ (wall 'x0) (wall 'y0)))
                   (to-center (vsub pos wall-point))
                   (distance (abs (dot to-center n)))
                   (penetration (- (ball 'radius) distance))
                   (direction (cond ((>= (dot to-center n) 0) n)
                                    (else (smul -1 n))))
                   (pos-corrected (cond ((> penetration 0) (vadd pos (smul penetration direction)))
                                        (else pos))))
              ((ball 'update) (pos-corrected 0) (pos-corrected 1) (v1 0) (v1 1))))
          (else ball))))

(define resolve-wall-collisions
  (lambda (balls walls)
    (map (lambda (ball)
           (foldl (lambda (b wall) (update-ball-with-wall b wall))
                  ball
                  walls))
         balls)))

(define resolve-ball-collisions
  (lambda (balls)
    (map (lambda (ball1)
           (foldl (lambda (acc ball2) (collide acc ball2))
                  ball1
                  balls))
         balls)))

(define advance-balls
  (lambda (balls walls dt)
    (map (lambda (ball) (gravitational-acceleration ball dt))
         (resolve-wall-collisions (resolve-ball-collisions balls) walls))))

(define render-scene
  (lambda (canvas window walls balls time)
    (do
      (fill canvas white)
      (let ((ctx (text-begin canvas)))
        (do
          (text-set-font ctx "SansSerif" 'normal 16)
          (text-set-color ctx 0 0 0 255)
          (text-draw ctx (- (/ width 2) 60) 18 (string-cat2 "COR: e ≈ " (string e)))
          (text-draw ctx 12 40 (format-sim-time time))
          (text-end ctx)))
      (map (lambda (wall) (display-wall canvas wall)) walls)
      (map (lambda (ball) (display-ball canvas ball)) balls)
      (refresh-window window))))

(define begin-recording
  (lambda (record? canvas fps output-path)
    (cond (record?
            (let ((file (make-file output-path)))
              (start-recording canvas fps file)))
          (else '#f))))

(define end-recording
  (lambda (record? canvas)
    (cond (record? (stop-recording canvas))
          (else '#t))))

(define record-frame
  (lambda (record? canvas)
    (cond (record? (encode-frame canvas))
          (else '#t))))

(define simulate-scenario
  (lambda (scenario)
    (lets ((canvas (create-graphics-device (scenario 'width) (scenario 'height)))
           (window (create-window canvas (scenario 'title)))
           (walls  (scenario 'walls))
           (duration (scenario 'duration))
           (dt (scenario 'time-step))
           (fps (scenario 'fps))
           (record? (scenario 'record?))
           (output (scenario 'output))
           (log-interval (scenario 'log-interval))
           (initial-balls (scenario 'balls))
           (sim-root simulations-root)
           (sim-folder (path-join sim-root simulation-output-folder))
           (video-path (path-join sim-folder (path-basename output))))
      (do
        (ensure-directory sim-root)
        (ensure-directory sim-folder)
        (begin-recording record? canvas fps video-path)
        (let loop ((time 0)
                   (balls initial-balls)
                   (step 0)
                   (ball-log '())
                   (total-log '()))
          (cond ((>= time duration)
                 (do
                   (end-recording record? canvas)
                   (print "simulation finished, compiling statistics…")
                   (let ((ordered-ball-log (reverse ball-log))
                         (ordered-total-log (reverse total-log)))
                     (do
                       (write-energy-csv sim-folder ordered-ball-log ordered-total-log)
                       '#t))
                   (print "artifacts generated in ")
                   (print sim-folder)
                   (print "done")
                   (close-window window)))
                (else
                  (lets ((ball-stats (capture-ball-stats time balls))
                         (total-stats (capture-total-stats time balls)))
                    (do
                      (render-scene canvas window walls balls time)
                      (record-frame record? canvas)
                      (maybe-log-energy log-interval step time balls)
                      (loop (+ time dt)
                            (advance-balls balls walls dt)
                            (+ step 1)
                            (flatten-ball-rows ball-stats ball-log)
                            (cons total-stats total-log)))))))))))

(define main
  (lambda (s)
    (simulate-scenario default-scenario)))
