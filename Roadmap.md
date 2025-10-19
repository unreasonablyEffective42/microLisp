# MicroLisp 1.0 Roadmap

This document outlines the development milestones required to bring MicroLisp to a stable 1.0 release.
Each phase represents a coherent stage of development, with estimated effort, commit counts, and major deliverables.

## Milestone 1 – Core Language Stabilization
Goal: Finalize evaluator semantics and ensure consistency across core syntax forms.

### Tasks:
- ~~Verify let, lets evaluation order, shadowing, and environment rules.~~
- Implement quasiquote, unquote, and unquote-splicing.
- Standardize argument arity checking across all built-ins.
- Improve error messages with consistent formatting and REPL colorization.
- Add test cases for nested quoting and sequential bindings.

## Milestone 2 – Standard Library Hardening
Goal: Expand and finalize the core standard library to provide essential utilities on par with classic Lisps.

### Tasks:
- Add string utilities: (substring s i j), (string-append ...), (string->list s), (list->string lst).
- Implement vector/list conversions: (list->vector lst) and (vector->list v).
- Extend math suite with (abs), (sqrt), (log), (exp) and exact simplifications where possible.
- Introduce (time-now), (sleep n), (random n), and (system cmd) primitives.
- Add comprehensive unit tests for new built-ins and edge cases.

## Milestone 3 – Numeric Tower Optimization
Goal: Improve numeric consistency, performance, and exactness.

### Tasks:
- Audit and correct promotion rules across all numeric types.
- Add exact simplifications for rational exponents, e.g. (^ 8 1/3) → 2.
- Cache common numeric constants (ZERO, ONE, PI, E).
- Inline small integer arithmetic paths for performance.
- Create a bench/ suite comparing MicroLisp vs. equivalent Java code.

## Milestone 4 – REPL and Runtime Polish
Goal: Improve the developer experience through a more interactive and informative REPL.

### Tasks:
- Add REPL meta-commands: :env, :reload, :quit, :help.
- ~~Support multi-line editing and syntax-colored output.~~ 
- Add (type-of x) for runtime type introspection.
- Include stack traces with line and source context.
- Ensure feature parity between java -jar and native-image builds.

## Milestone 5 – Module System and Documentation
Goal: Introduce module loading and produce full user and developer documentation.

### Tasks:
- Implement (import "file.scm") for lexical loading and evaluation.
- Add optional precompiled cache support (.mlc files).
- Write the Language Manual (docs/language.md) covering syntax, semantics, and built-ins.
- Write the Developer Guide (docs/architecture.md) describing internal structure and conventions.
- Include example programs (life.microlisp, fractals.microlisp, vector-demo.microlisp).

## Milestone 6 – Final Release Packaging
Goal: Package, tag, and publish the official MicroLisp 1.0 release.

### Tasks:
- Update build scripts (BUILD.sh, BUILD.bat) for reproducible builds.
- Add versioned ASCII banner and runtime version printing (MICROLISP v1.0).
- Write CHANGELOG.md summarizing key features and milestones.
- Tag and publish v1.0.0 release with both JAR and native builds.


