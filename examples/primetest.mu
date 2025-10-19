;a prime number stress test, do: microlisp lists.mu examples.mu primetest.mu to run
(define main
  (lambda (s)
    (print (sieve (lcomp 2 10000)))))
