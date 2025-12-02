(import lists)
(import strings)
(import "./src/lib/graphing.mu") ; use local graphing with configurable axes

(define csv-path "./simulations/sim-001/energies.csv")
(define output-path "./simulations/sim-001/energies-totals.png")

; Target plot size: very wide to stress rendering
(define plot-width 2400)
(define plot-height 720)

(define black (make-color 0 0 0))
(define red   (make-color 255 0 0))
(define green (make-color 0 200 70))
(define blue  (make-color 0 0 255))

(define list-count
  (lambda (xs)
    (let loop ((ys xs) (n 0))
      (cond ((null? ys) n)
            (else (loop (tail ys) (+ n 1)))))))

(define drop-until-empty
  (lambda (lines)
    (cond ((null? lines) '())
          ((null? (head lines)) (tail lines))
          (else (drop-until-empty (tail lines))))))

(define parse-totals
  (lambda (lines)
    ; lines should start after the totals header
    (let loop ((ls lines) (acc '()))
      (cond
        ((null? ls) (reverse acc))
        (else
          (let ((cells (split-by-comma (head ls))))
            (cond
              ((< (list-count cells) 4) (loop (tail ls) acc))
              (else
                (let ((cell-str (lambda (c) (list->string c))))
                  (let ((t (eval (cell-str (head cells))))
                        (k (eval (cell-str (head (tail cells)))))
                        (p (eval (cell-str (head (tail (tail cells))))))
                        (e (eval (cell-str (head (tail (tail (tail cells))))))))
                    (loop (tail ls) (cons (list t k p e) acc))))))))))))

(define series-from-rows
  (lambda (rows)
    (list
      (list 'total-kinetic   (map (lambda (row) (list (list-ref row 0) (list-ref row 1))) rows))
      (list 'total-potential (map (lambda (row) (list (list-ref row 0) (list-ref row 2))) rows))
      (list 'total-energy    (map (lambda (row) (list (list-ref row 0) (list-ref row 3))) rows)))))

(define ranges-from-series
  (lambda (series)
    (let loop ((entries series) (xmin '#f) (xmax '#f) (ymin '#f) (ymax '#f))
      (cond
        ((null? entries) (list xmin xmax ymin ymax))
        (else
          (let inner ((pts (head (tail (head entries)))) (xmin xmin) (xmax xmax) (ymin ymin) (ymax ymax))
            (cond
              ((null? pts) (loop (tail entries) xmin xmax ymin ymax))
              (else
                (let ((pt (head pts)))
                  (let ((x (list-ref pt 0))
                        (y (list-ref pt 1)))
                    (let ((nxmin (cond ((eq? xmin '#f) x) ((< x xmin) x) (else xmin)))
                          (nxmax (cond ((eq? xmax '#f) x) ((> x xmax) x) (else xmax)))
                          (nymin (cond ((eq? ymin '#f) y) ((< y ymin) y) (else ymin)))
                          (nymax (cond ((eq? ymax '#f) y) ((> y ymax) y) (else ymax))))
                      (inner (tail pts) nxmin nxmax nymin nymax)))))))))))

(define pad-range
  (lambda (min max pct)
    (let ((spread (- max min)))
      (let ((pad (* spread pct)))
        (list (- min pad) (+ max pad))))))

(define draw-series-line
  (lambda (canvas cfg points color)
    (lets ((px (make-px cfg))
           (py (make-py cfg)))
      (let loop ((pts points) (prev '#f))
        (cond
          ((null? pts) '#t)
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
                  (loop (tail pts) (list cx cy)))))))))))

(define main
  (lambda (s)
    (let ((lines (read-lines (make-file csv-path))))
      (cond
        ((null? lines)
         (do (print "CSV is empty") '#f))
        (else
          (let ((after-blank (drop-until-empty lines)))
            (cond
              ((null? after-blank)
               (do (print "No totals block found in CSV") '#f))
              (else
                (let ((data-lines (tail after-blank))) ; skip header
                  (let ((rows (parse-totals data-lines)))
                    (cond
                      ((null? rows)
                       (do (print "No total rows parsed") '#f))
                      (else
                        (let ((series (series-from-rows rows)))
                          (let* ((ranges (ranges-from-series series))
                                 (xmin (list-ref ranges 0))
                                 (xmax (list-ref ranges 1))
                                 (ymin (list-ref ranges 2))
                                 (ymax (list-ref ranges 3))
                                 (xp (pad-range xmin xmax 0.02))
                                 (yp (pad-range ymin ymax 0.05))
                                 (cfg (make-plot-config plot-width plot-height
                                                        (head xp) (head (tail xp))
                                                        (head yp) (head (tail yp))))
                                 (canvas (create-graphics-device (cfg-width cfg) (cfg-height cfg)))
                                 (ctx (text-begin canvas)))
                            (do
                              (fill canvas white)
                              (draw-axes canvas cfg)
                              (text-set-font ctx "SansSerif" 'normal 22)
                              (text-set-color ctx 0 0 0 255)
                              (text-draw ctx 40 40 "Simulation Energy Totals (from CSV)")
                              (text-end ctx)
                              (let loop ((entries series) (idx 0))
                                (cond
                                  ((null? entries) '#t)
                                  (else
                                    (let ((entry (head entries)))
                                      (let ((id (head entry))
                                            (pts (head (tail entry))))
                                        (let ((color (cond
                                                       ((eq? id 'total-kinetic) blue)
                                                       ((eq? id 'total-potential) red)
                                                       (else green))))
                                          (do
                                            (draw-series-line canvas cfg pts color)
                                            (loop (tail entries) (+ idx 1)))))))))
                              (let ((file (make-file output-path)))
                                (do
                                  (write-image canvas file)
                                  (print "saved plot to ")
                                  (print output-path)
                                  '#t))))))))))))))))
