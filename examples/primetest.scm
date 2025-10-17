;a prime number stress test, do: microlisp lists.scm examples.scm primetest.scm to run
(define main
  (lambda (s)
    (print (sieve (lcomp 2 10000)))))
