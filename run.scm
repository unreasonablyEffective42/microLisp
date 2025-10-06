(let loop ((a 0))
  (cond ((eq? a 10000000) "done")
        (else (loop (+ a 1)))))
