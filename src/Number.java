import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.MathContext;

public final class Number {
    enum Type { INT, BIGINT, FLOAT, BIGFLOAT, RATIONAL, BIGRATIONAL, COMPLEX }

    private final Type type;
    private final long intVal;
    private final BigInteger bigVal;
    private final double floatVal;
    private final BigDecimal bigFloatVal;
    private final long num;          // rational numerator
    private final long den;          // rational denominator
    private final BigInteger bigNum; // big rational numerator
    private final BigInteger bigDen; // big rational denominator
    private final Number real;       // complex real part
    private final Number imag;       // complex imaginary part

    private Number(Type type, long intVal, BigInteger bigVal,
                   double floatVal, BigDecimal bigFloatVal,
                   long num, long den,
                   BigInteger bigNum, BigInteger bigDen,
                   Number real, Number imag) {
        this.type = type;
        this.intVal = intVal;
        this.bigVal = bigVal;
        this.floatVal = floatVal;
        this.bigFloatVal = bigFloatVal;
        this.num = num;
        this.den = den;
        this.bigNum = bigNum;
        this.bigDen = bigDen;
        this.real = real;
        this.imag = imag;
    }

    // ---- Factory methods ----
    public static Number integer(long value) {
        return new Number(Type.INT, value, null, 0.0, null, 0, 0, null, null, null, null);
    }

    public static Number integer(BigInteger value) {
        return new Number(Type.BIGINT, 0, value, 0.0, null, 0, 0, null, null, null, null);
    }

    public static Number rational(long p, long q) {
        long g = gcd(p, q);
        p /= g;
        q /= g;
        if (q == 1) return Number.integer(p);
        return new Number(Type.RATIONAL, 0, null, 0.0, null, p, q, null, null, null, null);
    }

    public static Number bigRational(BigInteger p, BigInteger q) {
        BigInteger g = p.gcd(q);
        p = p.divide(g);
        q = q.divide(g);
        if (q.equals(BigInteger.ONE)) return Number.integer(p);
        return new Number(Type.BIGRATIONAL, 0, null, 0.0, null, 0, 0, p, q, null, null);
    }

    public static Number real(double value) {
        return new Number(Type.FLOAT, 0, null, value, null, 0, 0, null, null, null, null);
    }

    public static Number bigFloat(BigDecimal value) {
        return new Number(Type.BIGFLOAT, 0, null, 0.0, value, 0, 0, null, null, null, null);
    }

    public static Number complex(Number real, Number imag) {
        return new Number(Type.COMPLEX, 0, null, 0.0, null, 0, 0, null, null, real, imag);
    }

    // ---- gcd helper ----
    private static long gcd(long a, long b) {
        while (b != 0) {
            long temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    // ---- Addition ----
    public static Number add(Number a, Number b) {
        if (a.type.ordinal() < b.type.ordinal()) return add(b, a);

        switch (a.type) {
            case INT:         return addInt(a, b);
            case BIGINT:      return addBigInt(a, b);
            case RATIONAL:    return addRational(a, b);
            case BIGRATIONAL: return addBigRational(a, b);
            case FLOAT:       return addFloat(a, b);
            case BIGFLOAT:    return addBigFloat(a, b);            
            case COMPLEX:
                if (b.type != Type.COMPLEX)
                    b = Number.complex(b, Number.integer(0));
                return addComplex(a, b);
            default:
                throw new IllegalStateException("Unknown type: " + a.type);
        }
    }

    // ---- add helpers ----

    private static Number addInt(Number a, Number b) {
        // both a and b are INT or lower
        try {
            long sum = Math.addExact(a.intVal, b.intVal);
            return Number.integer(sum);
        } catch (ArithmeticException e) {
            BigInteger sum = BigInteger.valueOf(a.intVal).add(BigInteger.valueOf(b.intVal));
            return Number.integer(sum);
        }
    }

    private static Number addBigInt(Number a, Number b) {
        BigInteger left = (a.type == Type.BIGINT) ? a.bigVal : BigInteger.valueOf(a.intVal);
        BigInteger right = (b.type == Type.BIGINT) ? b.bigVal : BigInteger.valueOf(b.intVal);
        return Number.integer(left.add(right));
    }

    private static Number addFloat(Number a, Number b) {
        double left = a.floatVal;
        double right = (b.type == Type.FLOAT) ? b.floatVal : b.intVal;
        double sum = left + right;
        if (Double.isInfinite(sum)) {
            return addBigFloat(a, b);
        } else {
            return Number.real(sum);
        }
    }

    private static Number addBigFloat(Number a, Number b) {
        BigDecimal left = (a.type == Type.BIGFLOAT) ? a.bigFloatVal : BigDecimal.valueOf(a.floatVal);
        BigDecimal right = (b.type == Type.BIGFLOAT) ? b.bigFloatVal : BigDecimal.valueOf(b.floatVal);
        return Number.bigFloat(left.add(right));
    }

    private static Number addBigRational(Number a, Number b) {
        BigInteger num = a.bigNum.multiply(b.bigDen).add(b.bigNum.multiply(a.bigDen));
        BigInteger den = a.bigDen.multiply(b.bigDen);
        return Number.bigRational(num, den);
    }

    private static Number addComplex(Number a, Number b) {
        Number aReal = (a.real != null) ? a.real : Number.integer(0);
        Number aImag = (a.imag != null) ? a.imag : Number.integer(0);
        Number bReal = (b.real != null) ? b.real : Number.integer(0);
        Number bImag = (b.imag != null) ? b.imag : Number.integer(0);

        Number realPart = add(aReal, bReal);
        Number imagPart = add(aImag, bImag);
        return Number.complex(realPart, imagPart);
    }

    private static Number addRational(Number a, Number b) {
        long numerator = 0;
        long denominator = 0;

        switch (a.type) {
            case RATIONAL: {
                switch (b.type) {
                    case RATIONAL: {
                        try {
                            numerator = Math.addExact(
                                Math.multiplyExact(a.num, b.den),
                                Math.multiplyExact(b.num, a.den)
                            );
                            denominator = Math.multiplyExact(a.den, b.den);
                            return Number.rational(numerator, denominator);
                        } catch (ArithmeticException e) {
                            BigInteger numBig = BigInteger.valueOf(a.num)
                                .multiply(BigInteger.valueOf(b.den))
                                .add(BigInteger.valueOf(b.num)
                                .multiply(BigInteger.valueOf(a.den)));
                            BigInteger denBig = BigInteger.valueOf(a.den)
                                .multiply(BigInteger.valueOf(b.den));
                            return Number.bigRational(numBig, denBig);
                        }
                    }

                    case INT: {
                        try {
                            numerator = Math.addExact(Math.multiplyExact(b.intVal, a.den), a.num);
                            denominator = a.den;
                            return Number.rational(numerator, denominator);
                        } catch (ArithmeticException e) {
                            BigInteger anum = BigInteger.valueOf(a.num);
                            BigInteger aden = BigInteger.valueOf(a.den);
                            BigInteger bnum = BigInteger.valueOf(b.intVal);
                            BigInteger numBig = anum.add(bnum.multiply(aden));
                            return Number.bigRational(numBig, aden);
                        }
                    }

                    case BIGINT: {
                        BigInteger anum = BigInteger.valueOf(a.num);
                        BigInteger aden = BigInteger.valueOf(a.den);
                        BigInteger bnum = b.bigVal;
                        BigInteger numBig = anum.add(bnum.multiply(aden));
                        return Number.bigRational(numBig, aden);
                    }

                    case BIGRATIONAL: {
                        BigInteger anum = BigInteger.valueOf(a.num);
                        BigInteger aden = BigInteger.valueOf(a.den);
                        BigInteger numBig = anum.multiply(b.bigDen)
                            .add(b.bigNum.multiply(aden));
                        BigInteger denBig = aden.multiply(b.bigDen);
                        return Number.bigRational(numBig, denBig);
                    }

                    case FLOAT: {
                        double r = ((double) a.num / (double) a.den) + b.floatVal;
                        return Number.real(r);
                    }

                    case BIGFLOAT: {
                        BigDecimal left = BigDecimal.valueOf(a.num)
                            .divide(BigDecimal.valueOf(a.den), MathContext.DECIMAL128);
                        BigDecimal sum = left.add(b.bigFloatVal);
                        return Number.bigFloat(sum);
                    }

                    case COMPLEX: {
                        Number realPart = addRational(a, b.real);
                        return Number.complex(realPart, b.imag);
                    }

                    default:
                        throw new IllegalArgumentException("Unsupported type combination: RATIONAL + " + b.type);
                }
            }

            default:
                throw new IllegalArgumentException("Invalid call: addRational with non-rational lhs " + a.type);
        }
    }

    @Override
    public String toString() {
        return switch (type) {
            case INT          -> Long.toString(intVal);
            case BIGINT       -> bigVal.toString();
            case FLOAT        -> Double.toString(floatVal);
            case BIGFLOAT     -> bigFloatVal.toPlainString();
            case RATIONAL     -> (den == 1) ? Long.toString(num) : "(" + num + "/" + den + ")";
            case BIGRATIONAL  -> (bigDen.equals(BigInteger.ONE)) ? bigNum.toString() : "(" + bigNum + "/" + bigDen + ")";
            case COMPLEX      -> real + " + " + imag + "i";
        };
    }


public static void main(String[] args) {
    System.out.println("=== BASIC TYPES ===");
    Number i  = Number.integer(100);
    Number k  = Number.integer(new BigInteger("6000000000000000000000"));
    Number f  = Number.real(5300.12347);
    Number bf = Number.bigFloat(new BigDecimal("123456789.987654321"));
    Number r  = Number.rational(300, 200); // 3/2
    Number br = Number.bigRational(new BigInteger("12345678901234567890"),
                                   new BigInteger("9876543210987654321"));
    Number c  = Number.complex(r, i);

    System.out.println("INT:        " + i);
    System.out.println("BIGINT:     " + k);
    System.out.println("FLOAT:      " + f);
    System.out.println("BIGFLOAT:   " + bf);
    System.out.println("RATIONAL:   " + r);
    System.out.println("BIGRATIONAL:" + br);
    System.out.println("COMPLEX:    " + c);
    System.out.println();

    System.out.println("=== SIMPLE ADDITION TESTS ===");
    System.out.println("int + int            = " + Number.add(Number.integer(5), Number.integer(7)) + "   (expected 12)");
    System.out.println("bigint + int         = " + Number.add(k, i) + "   (expected 6000000000000000000100)");
    System.out.println("float + float        = " + Number.add(Number.real(1.25), Number.real(3.75)) + "   (expected 5.0)");
    System.out.println("rational + rational  = " + Number.add(Number.rational(1, 2), Number.rational(1, 3)) + "   (expected 5/6)");
    System.out.println("rational + int       = " + Number.add(Number.rational(3, 2), Number.integer(2)) + "   (expected 7/2)");
    System.out.println("int + rational       = " + Number.add(Number.integer(2), Number.rational(3, 2)) + "   (expected 7/2)");
    System.out.println();

    System.out.println("=== BIG PROMOTIONS ===");
    // overflow test for int + int -> bigint
    Number big1 = Number.integer(Long.MAX_VALUE);
    Number big2 = Number.integer(1);
    System.out.println("overflow test (long + 1) = " + Number.add(big1, big2) + "   (expected 9223372036854775808 as BigInteger)");

    // rational overflow to bigRational
    Number largeR1 = Number.rational(Long.MAX_VALUE / 2, 3);
    Number largeR2 = Number.rational(Long.MAX_VALUE / 2, 3);
    System.out.println("rational overflow => bigRational = " + Number.add(largeR1, largeR2));
    System.out.println();

    System.out.println("=== MIXED ADDITION TESTS ===");
    System.out.println("float + int          = " + Number.add(Number.real(2.5), Number.integer(3)) + "   (expected 5.5)");
    System.out.println("float + rational     = " + Number.add(Number.real(0.5), Number.rational(1, 3)) + "   (expected ~0.8333)");
    System.out.println("bigfloat + float     = " + Number.add(bf, f) + "   (expected ~123462090.1111)");
    System.out.println("rational + bigfloat  = " + Number.add(r, bf) + "   (expected ~123456791.487654321)");
    System.out.println();

    System.out.println("=== COMPLEX ADDITION ===");
    Number c1 = Number.complex(Number.rational(1, 2), Number.integer(2));
    Number c2 = Number.complex(Number.integer(3), Number.integer(4));
    System.out.println("complex + complex    = " + Number.add(c1, c2) + "   (expected (7/2 + 6i))");
    System.out.println("complex + rational   = " + Number.add(c1, Number.rational(1, 2)) + "   (expected (1 + 2i))");
    System.out.println("complex + int        = " + Number.add(c1, Number.integer(1)) + "   (expected (3/2 + 2i))");
    System.out.println();

    System.out.println("=== BIGFLOAT PRECISION ===");
    BigDecimal veryBig = new BigDecimal("1.0000000000000000000000000000000001");
    System.out.println("bigfloat + bigfloat  = " + Number.add(Number.bigFloat(veryBig), Number.bigFloat(veryBig)));
    System.out.println();

    System.out.println("=== EDGE CASES ===");
    System.out.println("zero + zero          = " + Number.add(Number.integer(0), Number.integer(0)));
    System.out.println("negatives            = " + Number.add(Number.integer(-5), Number.integer(2)) + "   (expected -3)");
    System.out.println("rational + negative  = " + Number.add(Number.rational(1, 3), Number.integer(-1)) + "   (expected -2/3)");
}

}
