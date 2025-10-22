@FunctionalInterface
interface TriFunction<A, B, C, R> {
    R apply(A a, B b, C c);
}

@FunctionalInterface
interface QuadFunction<A, B, C, D, R> {
    R apply(A a, B b, C c, D d);
}

@FunctionalInterface
interface PentaFunction<A, B, C, D, E, R> {
    R apply(A a, B b, C c, D d, E e);
}

@FunctionalInterface
interface HexFunction<A,B,C,D,E,F,R> {
    R apply(A a, B b, C c, D d, E e, F f);
}

@FunctionalInterface
interface BiConsumer<A, B> {
    void accept(A a, B b);
}

@FunctionalInterface
interface TriConsumer<A, B, C> {
    void accept(A a, B b, C c);
}
