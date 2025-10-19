• Global Prelude

  - else is bound to #t for cond default guards 
  - Type predicates null?, number?, symbol? return #t/#f on emptiness or runtime tags
  - Parity helpers even? and odd?
  - Boolean ops !, not, and, or, xor operate on #t/#f with short-circuit semantics
  - Sequence accessors head, tail, length, plus constructor cons 
  - Output and meta tools print, printf, numeric conversions to-inexact, to-inexact-big,and 
    eval (string → parsed AST → evaluated value) round out the core interface 

• Arithmetic & Comparison

  - Variadic arithmetic +, -, *, / consume MicroLisp numbers, enforce type checks, and supply
    identity defaults for empty inputs where defined 
  - Binary operators % and ^ delegate to modular exponentiation helpers in Number
  - Relational predicates < and > compare numeric tower values, returning booleans as #t/#f 
  - Equality = performs numeric, string, and list comparisons; eq? also supports symbol identity
    and BigInteger equality, raising arity errors when misused

• Extended Runtime Libraries

  - Vector utilities size and vector expose fixed-size array support backed by Vector.of and
    linked-list conversion
  - File helpers read, read-from-file, make-file, write-to-file cover REPL evaluation of a line,
    file -> char-list reads, file creation, and text writes (expecting a File handle and char-list
    payload) 
  - Pixel graphics frame adds window/device lifecycle (create-window, refresh-window, close-
    window, create-graphics-device), image persistence (write-image), color constructors (make-
    color, make-rgba), drawing primitives (draw-pixel, fill), timing (wait), and dimension
    accessors (image-width, image-height)

  Optional lists.scm Utilities

  - Tail recursive map, filter, foldl, and foldr, giving higher-order list
    transforms built atop head, tail, cons, and reverse
  - A tail-recursive reverse and range builder lcomp support list construction patterns 
  - chars->string converts character lists to strings via folding, complementing the core print/
    printf behavior 
  - words tokenizes char lists on whitespace, returning lists of strings using the helpers above

  Load additional example files the same way to extend the environment; the interpreter only
  preloads the Java-defined bindings unless you :load or pass scheme sources on startup.
