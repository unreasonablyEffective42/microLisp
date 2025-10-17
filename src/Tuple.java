public abstract class Tuple {
    public abstract int size();
    public abstract Object get(int index);
    @Override public abstract String toString();

    public static Tuple of(Object a, Object b) { return new Tuple2(a, b); }
    public static Tuple of(Object a, Object b, Object c) { return new Tuple3(a, b, c); }
    public static Tuple of(Object a, Object b, Object c, Object d) { return new Tuple4(a, b, c, d); }
    public static Tuple of(Object a, Object b, Object c, Object d, Object e) { return new Tuple5(a, b, c, d, e); }
    public static Tuple of(Object a, Object b, Object c, Object d, Object e, Object f) { return new Tuple6(a, b, c, d, e, f); }
    public static Tuple of(Object a, Object b, Object c, Object d, Object e, Object f, Object g) { return new Tuple7(a, b, c, d, e, f, g); }
    public static Tuple of(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h) { return new Tuple8(a, b, c, d, e, f, g, h); }
    public static Tuple of(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i) { return new Tuple9(a, b, c, d, e, f, g, h, i); }

    protected static void checkIndex(int index, int size) {
        if (index < 0 || index >= size) {
            throw outOfRange(index, size);
        }
    }

    protected static IndexOutOfBoundsException outOfRange(int index, int size) {
        return new IndexOutOfBoundsException("Tuple index " + index + " out of range [0," + (size - 1) + "]");
    }

    protected static String formatTuple(Object... values) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(values[i]);
        }
        sb.append(')');
        return sb.toString();
    }

    public static final class Tuple2 extends Tuple {
        public final Object a, b;
        public Tuple2(Object a, Object b) { this.a = a; this.b = b; }
        @Override public int size() { return 2; }
        @Override public Object get(int index) {
            checkIndex(index, 2);
            return (index == 0) ? a : b;
        }
        @Override public String toString() { return formatTuple(a, b); }
    }

    public static final class Tuple3 extends Tuple {
        public final Object a, b, c;
        public Tuple3(Object a, Object b, Object c) { this.a = a; this.b = b; this.c = c; }
        @Override public int size() { return 3; }
        @Override public Object get(int index) {
            checkIndex(index, 3);
            return switch (index) {
                case 0 -> a;
                case 1 -> b;
                case 2 -> c;
                default -> throw outOfRange(index, 3);
            };
        }
        @Override public String toString() { return formatTuple(a, b, c); }
    }

    public static final class Tuple4 extends Tuple {
        public final Object a, b, c, d;
        public Tuple4(Object a, Object b, Object c, Object d) { this.a = a; this.b = b; this.c = c; this.d = d; }
        @Override public int size() { return 4; }
        @Override public Object get(int index) {
            checkIndex(index, 4);
            return switch (index) {
                case 0 -> a;
                case 1 -> b;
                case 2 -> c;
                case 3 -> d;
                default -> throw outOfRange(index, 4);
            };
        }
        @Override public String toString() { return formatTuple(a, b, c, d); }
    }

    public static final class Tuple5 extends Tuple {
        public final Object a, b, c, d, e;
        public Tuple5(Object a, Object b, Object c, Object d, Object e) { this.a = a; this.b = b; this.c = c; this.d = d; this.e = e; }
        @Override public int size() { return 5; }
        @Override public Object get(int index) {
            checkIndex(index, 5);
            return switch (index) {
                case 0 -> a;
                case 1 -> b;
                case 2 -> c;
                case 3 -> d;
                case 4 -> e;
                default -> throw outOfRange(index, 5);
            };
        }
        @Override public String toString() { return formatTuple(a, b, c, d, e); }
    }

    public static final class Tuple6 extends Tuple {
        public final Object a, b, c, d, e, f;
        public Tuple6(Object a, Object b, Object c, Object d, Object e, Object f) { this.a = a; this.b = b; this.c = c; this.d = d; this.e = e; this.f = f; }
        @Override public int size() { return 6; }
        @Override public Object get(int index) {
            checkIndex(index, 6);
            return switch (index) {
                case 0 -> a;
                case 1 -> b;
                case 2 -> c;
                case 3 -> d;
                case 4 -> e;
                case 5 -> f;
                default -> throw outOfRange(index, 6);
            };
        }
        @Override public String toString() { return formatTuple(a, b, c, d, e, f); }
    }

    public static final class Tuple7 extends Tuple {
        public final Object a, b, c, d, e, f, g;
        public Tuple7(Object a, Object b, Object c, Object d, Object e, Object f, Object g) { this.a = a; this.b = b; this.c = c; this.d = d; this.e = e; this.f = f; this.g = g; }
        @Override public int size() { return 7; }
        @Override public Object get(int index) {
            checkIndex(index, 7);
            return switch (index) {
                case 0 -> a;
                case 1 -> b;
                case 2 -> c;
                case 3 -> d;
                case 4 -> e;
                case 5 -> f;
                case 6 -> g;
                default -> throw outOfRange(index, 7);
            };
        }
        @Override public String toString() { return formatTuple(a, b, c, d, e, f, g); }
    }

    public static final class Tuple8 extends Tuple {
        public final Object a, b, c, d, e, f, g, h;
        public Tuple8(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h) {
            this.a = a; this.b = b; this.c = c; this.d = d; this.e = e; this.f = f; this.g = g; this.h = h;
        }
        @Override public int size() { return 8; }
        @Override public Object get(int index) {
            checkIndex(index, 8);
            return switch (index) {
                case 0 -> a;
                case 1 -> b;
                case 2 -> c;
                case 3 -> d;
                case 4 -> e;
                case 5 -> f;
                case 6 -> g;
                case 7 -> h;
                default -> throw outOfRange(index, 8);
            };
        }
        @Override public String toString() { return formatTuple(a, b, c, d, e, f, g, h); }
    }

    public static final class Tuple9 extends Tuple {
        public final Object a, b, c, d, e, f, g, h, i;
        public Tuple9(Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i) {
            this.a = a; this.b = b; this.c = c; this.d = d; this.e = e; this.f = f; this.g = g; this.h = h; this.i = i;
        }
        @Override public int size() { return 9; }
        @Override public Object get(int index) {
            checkIndex(index, 9);
            return switch (index) {
                case 0 -> a;
                case 1 -> b;
                case 2 -> c;
                case 3 -> d;
                case 4 -> e;
                case 5 -> f;
                case 6 -> g;
                case 7 -> h;
                case 8 -> i;
                default -> throw outOfRange(index, 9);
            };
        }
        @Override public String toString() { return formatTuple(a, b, c, d, e, f, g, h, i); }
    }

    public static Tuple of(Object... args) {
        int n = args.length;
        return switch (n) {
            case 0 -> throw new IllegalArgumentException("Empty tuple not supported");
            case 1 -> throw new IllegalArgumentException("Single-element tuple not supported");
            case 2 -> new Tuple2(args[0], args[1]);
            case 3 -> new Tuple3(args[0], args[1], args[2]);
            case 4 -> new Tuple4(args[0], args[1], args[2], args[3]);
            case 5 -> new Tuple5(args[0], args[1], args[2], args[3], args[4]);
            case 6 -> new Tuple6(args[0], args[1], args[2], args[3], args[4], args[5]);
            case 7 -> new Tuple7(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
            case 8 -> new Tuple8(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
            case 9 -> new Tuple9(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
            default -> throw new IllegalArgumentException("Tuple size > 9 not supported");
        };
    }
}
