(import lists)
(import strings)

(define csv-path "./simulations/sim-001/energies.csv")
(define output-path "./simulations/sim-001/test-overall.png")

(define width 900)
(define height 600)
(define margin 50)

(define black (make-color 0 0 0))
(define red   (make-color 255 0 0))
(define green (make-color 0 255 50))
(define blue  (make-color 0 0 255))

(define split-lines
  (lambda (chars)
    (let loop ((xs chars) (current '()) (lines '()))
      (cond ((null? xs) (reverse (cons (reverse current) lines)))
            ((eq? (head xs) "\r") (loop (tail xs) current lines))
            ((eq? (head xs) "\n")
             (loop (tail xs) '() (cons (reverse current) lines)))
            (else (loop (tail xs) (cons (head xs) current) lines))))))

(define drop-until-empty
  (lambda (lines)
    (cond ((null? lines) '())
          ((null? (head lines)) (tail lines))
          (else (drop-until-empty (tail lines))))))

(define split-csv-line
  (lambda (line)
    (let loop ((chars line) (current '()) (cells '()))
      (cond ((null? chars) (reverse (cons (reverse current) cells)))
            ((eq? (head chars) ",")
             (loop (tail chars) '() (cons (reverse current) cells)))
            (else (loop (tail chars) (cons (head chars) current) cells))))))

(define parse-number
  (lambda (chars)
    (read (string->list (list->string chars)))))

(define parse-total-rows
  (lambda (lines)
    (cond ((null? lines) '())
          (else
            (let ((data (tail lines)))
              (map (lambda (line)
                     (let ((cells (split-csv-line line)))
                       (list (parse-number (list-ref cells 0))
                             (parse-number (list-ref cells 1))
                             (parse-number (list-ref cells 2))
                             (parse-number (list-ref cells 3)))))
                   data))))))

(define make-series
  (lambda (rows)
    (list
      (list 'total-kinetic (map (lambda (row) (list (list-ref row 0) (list-ref row 1))) rows))
      (list 'total-potential (map (lambda (row) (list (list-ref row 0) (list-ref row 2))) rows))
      (list 'total-energy (map (lambda (row) (list (list-ref row 0) (list-ref row 3))) rows)))))

(define make-scale
  (lambda (xmin xmax ymin ymax)
    (let ((inner-width (- width (* 2 margin)))
          (inner-height (- height (* 2 margin))))
      (let ((spread-x (- xmax xmin))
            (spread-y (- ymax ymin)))
        (list (lambda (value)
                (+ margin (* inner-width (/ (- value xmin)
                                            (cond ((= spread-x 0) 1)
                                                  (else spread-x))))))
              (lambda (value)
                (- (- height margin)
                   (* inner-height (/ (- value ymin)
                                      (cond ((= spread-y 0) 1)
                                            (else spread-y)))))))))))

(define series-ranges
  (lambda (series)
    (let loop ((entries series) (xmin '#f) (xmax '#f) (ymin '#f) (ymax '#f))
      (cond ((null? entries) (list xmin xmax ymin ymax))
            (else
              (let inner ((points (head (tail (head entries))))
                          (xmin xmin) (xmax xmax) (ymin ymin) (ymax ymax))
                (cond ((null? points) (loop (tail entries) xmin xmax ymin ymax))
                      (else
                        (let ((pt (head points)))
                          (let ((x (list-ref pt 0))
                                (y (list-ref pt 1)))
                            (let ((new-xmin (cond ((eq? xmin '#f) x) ((< x xmin) x) (else xmin)))
                                  (new-xmax (cond ((eq? xmax '#f) x) ((> x xmax) x) (else xmax)))
                                  (new-ymin (cond ((eq? ymin '#f) y) ((< y ymin) y) (else ymin)))
                                  (new-ymax (cond ((eq? ymax '#f) y) ((> y ymax) y) (else ymax))))
                              (inner (tail points) new-xmin new-xmax new-ymin new-ymax)))))))))))

(define draw-axes
  (lambda (canvas)
    (do
      (lines canvas margin (- height margin)
             (- width margin) (- height margin) black)
      (lines canvas margin margin
             margin (- height margin) black))))

(define draw-series-line
  (lambda (canvas points color px py)
    (let loop ((pts points) (prev '#f))
      (cond ((null? pts) '#t)
            ((eq? prev '#f)
             (let ((pt (head pts)))
               (loop (tail pts)
                     (list (floor (px (list-ref pt 0)))
                           (floor (py (list-ref pt 1)))))))
            (else
              (let ((pt (head pts)))
                (let ((cx (floor (px (list-ref pt 0))))
                      (cy (floor (py (list-ref pt 1)))))
                  (do
                    (lines canvas
                           (list-ref prev 0)
                           (list-ref prev 1)
                           cx
                           cy
                           color)
                    (loop (tail pts) (list cx cy))))))))))

(define plot-totals
  (lambda (series colors)
    (let ((ranges (series-ranges series)))
      (let ((xmin (list-ref ranges 0))
            (xmax (list-ref ranges 1))
            (ymin (list-ref ranges 2))
            (ymax (list-ref ranges 3)))
        (let ((canvas (create-graphics-device width height))
              (scale (make-scale xmin xmax ymin ymax))
              (ctx (text-begin canvas)))
          (lets ((px (head scale))
                 (py (head (tail scale))))
            (do
              (fill canvas (make-color 255 255 255))
              (draw-axes canvas)
              (text-set-font ctx "SansSerif" 'normal 18)
              (text-set-color ctx 0 0 0 255)
              (text-draw ctx 40 30 "Simulation Energy Totals (from CSV)")
              (text-end ctx)
              (print "series count: ")
              (print (length series))
              (let loop ((entries series) (index 0))
                (cond ((null? entries) '#t)
                      (else
                        (let ((entry (head entries)))
                          (let ((id (head entry))
                                (points (head (tail entry)))
                                (color (cond ((eq? id 'total-kinetic) blue)
                                             ((eq? id 'total-potential) red)
                                             (else green))))
                            (do
                              (print "drawing series ")
                              (print index)
                              (draw-series-line canvas points color px py)
                              (loop (tail entries) (+ index 1))))))))
              (let ((file (make-file output-path)))
                (write-image canvas file))))))))))

(define main
  (lambda (s)
    (let ((content (read-from-file csv-path)))
      (let ((lines (split-lines content)))
        (let ((totals (drop-until-empty lines)))
          (cond ((null? totals) (print "no totals in csv"))
                (else
                  (let ((rows (parse-total-rows totals)))
                    (do
                      (print "plotting totals from csv…")
                      (plot-totals (make-series rows) '())
                      (print "plot saved to ")
                      (print output-path))))))))))
