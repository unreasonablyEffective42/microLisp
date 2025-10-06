(let ((x 1))
  (do 
    (print x)
    (let ((x 2))
      (print x)
    )
    (print x)))
