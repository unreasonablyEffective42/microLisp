(define background (make-color 12 14 32))
(define shadow-rgb (:: 20 20 20))
(define accent-rgb (:: 113 182 255))
(define accent-2-rgb (:: 255 153 85))
(define text-rgb (:: 240 240 240))

(define draw-title
  (lambda (ctx)
    (do
      (text-set-font ctx "SansSerif" 'bold 48)
      (text-set-color ctx (shadow-rgb 0) (shadow-rgb 1) (shadow-rgb 2) 255)
      (text-draw ctx 36 102 "MicroLisp")
      (text-set-color ctx (accent-rgb 0) (accent-rgb 1) (accent-rgb 2) 255)
      (text-draw ctx 30 96 "MicroLisp"))))

(define draw-subtitle
  (lambda (ctx)
    (lets ((metrics (text-measure ctx "Text Graphics"))
           (line-height (metrics 1))
           (baseline (metrics 2)))
      (do
        (text-set-font ctx "SansSerif" 'italic 26)
        (text-set-color ctx (accent-2-rgb 0) (accent-2-rgb 1) (accent-2-rgb 2) 255)
        (text-draw ctx 32 (+ 120 line-height) "Text Graphics")
        (text-set-font ctx "Monospaced" 'plain 16)
        (text-set-color ctx (text-rgb 0) (text-rgb 1) (text-rgb 2) 255)
        (text-draw ctx 32 (+ 120 line-height baseline 24)
                   "text-measure => (width height baseline)")))))

(define main
  (lambda (flag)
    (lets ((canvas (create-graphics-device 420 220))
           (window (create-window canvas "text-demo"))
           (ctx (text-begin canvas))
           (file (make-file "../images/text-demo.png")))
      (do
        (fill canvas background)
        (draw-title ctx)
        (draw-subtitle ctx)
        (text-end ctx)
        (refresh-window window)        
        (write-image canvas file)


        ))))
