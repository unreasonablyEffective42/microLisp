; ============================================================
; Symbolic Differentiation for MicroLisp 
; ============================================================

(define diff
  (lambda (expr var)
    (cond
      ; ---- Base cases ----
      ((number? expr) 0)
      ((symbol? expr)
       (cond ((eq? expr var) 1)
             (else 0)))

      ; ---- Recursive pattern match ----
      ((eq? (head expr) '+)
       (list '+ (diff (head (tail expr)) var)
                 (diff (head (tail (tail expr))) var)))

      ((eq? (head expr) '-)
       (list '- (diff (head (tail expr)) var)
                 (diff (head (tail (tail expr))) var)))

      ((eq? (head expr) '*)
       (let ((u (head (tail expr)))
             (v (head (tail (tail expr)))))
         (list '+ (list '* (diff u var) v)
                   (list '* u (diff v var)))))

      ((eq? (head expr) '/)
       (let ((u (head (tail expr)))
             (v (head (tail (tail expr)))))
         (list '/ (list '- (list '* (diff u var) v)
                            (list '* u (diff v var)))
                   (list '^ v 2))))

      ((eq? (head expr) '^)
       (let ((base (head (tail expr)))
             (exponent (head (tail (tail expr)))))
         (cond ((number? exponent)
                (list '* exponent (list '^ base (- exponent 1))))
               (else
                (list '* (list '^ base exponent)
                       (list '+ (list '* (diff exponent var) (list 'ln base))
                                 (list '* exponent (list '/ (diff base var) base))))))))

      (else "Unsupported expression type for diff"))))

; ============================================================
; Simplifier
; ============================================================

(define simplify
  (lambda (expr)
    (cond
      ((number? expr) expr)
      ((symbol? expr) expr)
      ((eq? (head expr) '+)
       (let ((a (simplify (head (tail expr))))
             (b (simplify (head (tail (tail expr))))))
         (cond ((= a 0) b)
               ((= b 0) a)
               (else (list '+ a b)))))
      ((eq? (head expr) '*)
       (let ((a (simplify (head (tail expr))))
             (b (simplify (head (tail (tail expr))))))
         (cond ((or (= a 0) (= b 0)) 0)
               ((= a 1) b)
               ((= b 1) a)
               (else (list '* a b)))))
      (else expr))))

; ============================================================
; Examples
; ============================================================
(print "Example 1:(^ x 10/3)")
(print (diff '(^ x 10/3) 'x))

(print "Example 2:(* x (^ x 3))") 
(print (diff '(* x (^ x 3)) 'x))

(print  "Example 3 (example 2 simplified):")
(print (simplify (diff '(* x (^ x 3)) 'x)))

(print "Example 4 (quotient rule):")
(print (diff '(/ (^ x 2) (+ x 1)) 'x))
