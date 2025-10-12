(let loop ((a 0))
  (cond ((eq? a 100000000) (print a))
        (else (loop (+ a 1)))))
