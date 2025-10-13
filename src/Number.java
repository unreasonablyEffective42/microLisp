/*
 * Scheme-style numeric tower supporting both exact and inexact arithmetic,
 * with automatic promotion and mixed-type interoperability.
 *
 * Efficiency is achieved by using primitive long and double representations
 * until promotion to BigInteger or BigDecimal is required. 
 *
 * All arithmetic operations between any of the Number variants (defined in the
 * Type enum) are supported and will preserve the correct notion of exactness
 * or inexactness at the highest precision necessary.
 *
 * The numeric tower hierarchy (promotion direction):
 *
 *
 *                ┌─────────┐
 *                │   INT   │  (ℤ exact)
 *                └────┬────┘
 *                     │
 *                     ▼
 *                ┌─────────┐
 *                │  BIGINT │  (arbitrary ℤ)
 *                └────┬────┘
 *                     │
 *  ┌────────────┬─────┴──────┬────────────┐
 *  │            │            │            │
 *  ▼            ▼            ▼            ▼
 * RATIONAL  BIGRATIONAL     FLOAT      BIGFLOAT
 *(ℚ exact)  (ℚ exact)   (ℝ inexact)  (ℝ 'exact')
 *  └────────────┬────────────┬────────────┘
 *               │            │
 *               ▼            ▼
 *              ┌──────────────┐
 *              │   COMPLEX    │  (ℂ, 2D field)
 *              └──────┬───────┘
 *                     │
 *                     ▼
 *              ┌──────────────┐
 *              │ QUATERNION   │  (ℍ, 4D division ring)
 *              └──────────────┘
 */

import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class Number {
    enum Type { INT, BIGINT, FLOAT, BIGFLOAT, RATIONAL, BIGRATIONAL, COMPLEX, QUATERNION }

    public final Type type;
    public final long intVal;
    public final BigInteger bigVal;
    public final double floatVal;
    public final BigDecimal bigFloatVal;
    public final long num;          // rational numerator
    public final long den;          // rational denominator
    public final BigInteger bigNum; // big rational numerator
    public final BigInteger bigDen; // big rational denominator
    public final Number real;       // real component
    public final Number ipart;      // i component
    public final Number jpart;      // j component
    public final Number kpart;      // k component

    // ------ Cached constants ------
    private static final Number ZERO_INT         = Number.integer(0);
    private static final Number ZERO_BIGINT      = Number.integer(BigInteger.ZERO);
    private static final Number ZERO_FLOAT       = Number.real(0.0);
    private static final Number ZERO_BIGFLOAT    = Number.real(BigDecimal.ZERO);
    private static final Number ZERO_RATIONAL    = Number.rational(0, 1);
    private static final Number ZERO_BIGRATIONAL = Number.rational(BigInteger.ZERO, BigInteger.ONE);
    private static final Number ZERO_COMPLEX     = Number.complex(ZERO_INT, ZERO_INT);
    private static final Number ZERO_QUATERNION  = Number.quaternion(ZERO_INT, ZERO_INT, ZERO_INT, ZERO_INT);

    private static final Number ONE_INT          = Number.integer(1);
    private static final Number ONE_BIGINT       = Number.integer(BigInteger.ONE);
    private static final Number ONE_FLOAT        = Number.real(1.0);
    private static final Number ONE_BIGFLOAT     = Number.real(BigDecimal.ONE);
    private static final Number ONE_RATIONAL     = Number.rational(1, 1);
    private static final Number ONE_BIGRATIONAL  = Number.rational(BigInteger.ONE, BigInteger.ONE);
    private static final Number ONE_COMPLEX      = Number.complex(ONE_INT, ZERO_INT);
    private static final Number ONE_QUATERNION   = Number.quaternion(ONE_INT, ZERO_INT, ZERO_INT, ZERO_INT);


    
    private Number(Type type, long intVal, BigInteger bigVal,
                double floatVal, BigDecimal bigFloatVal,
                long num, long den,
                BigInteger bigNum, BigInteger bigDen,
                Number real, Number ipart, Number jpart, Number kpart) {
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
        this.ipart = ipart;
        this.jpart = jpart;
        this.kpart = kpart;
    }

    // ---- Factory methods ----
    
    public static Number integer(long value) {
        return new Number(Type.INT, value, null, 0.0, null, 0, 0, null, null, null, null, null, null);
    }

    public static Number integer(BigInteger value) {
        return new Number(Type.BIGINT, 0, value, 0.0, null, 0, 0, null, null, null, null, null, null);
    }

    public static Number rational(long p, long q) {
        if (q == 0)
            throw new ArithmeticException("Division by zero in rational constructor");
        long g = gcd(p, q);
        p /= g;
        q /= g;
        if (q < 0) {
            p = -p;
            q = -q;
        }
        if (q == 1) return Number.integer(p);
        return new Number(Type.RATIONAL, 0, null, 0.0, null, p, q, null, null, null, null, null, null);
    }

    public static Number rational(BigInteger p, BigInteger q) {
        if (q.signum() == 0)
            throw new ArithmeticException("Division by zero in rational constructor");
        BigInteger g = p.gcd(q);
        p = p.divide(g);
        q = q.divide(g);
        if (q.signum() < 0) {
            p = p.negate();
            q = q.negate();
        }
        if (q.equals(BigInteger.ONE)) return Number.integer(p);
        return new Number(Type.BIGRATIONAL, 0, null, 0.0, null, 0, 0, p, q, null, null, null, null);
    }

    public static Number rational(Number numerator, Number denominator) {
        if (numerator.type != Type.INT && numerator.type != Type.BIGINT)
            throw new IllegalArgumentException("numerator must be INT or BIGINT, got " + numerator.type);
        if (denominator.type != Type.INT && denominator.type != Type.BIGINT)
            throw new IllegalArgumentException("denominator must be INT or BIGINT, got " + denominator.type);

        if ((denominator.type == Type.INT && denominator.intVal == 0) ||
            (denominator.type == Type.BIGINT && denominator.bigVal.signum() == 0)) {
            throw new ArithmeticException("Division by zero in rational constructor");
        }

        boolean numInt = numerator.type == Type.INT;
        boolean denInt = denominator.type == Type.INT;

        if (numInt && denInt) {
            return Number.rational(numerator.intVal, denominator.intVal);
        }

        BigInteger num = numInt ? BigInteger.valueOf(numerator.intVal) : numerator.bigVal;
        BigInteger den = denInt ? BigInteger.valueOf(denominator.intVal) : denominator.bigVal;
        return Number.rational(num, den);
    }

    public static Number real(double value) {
        return new Number(Type.FLOAT, 0, null, value, null, 0, 0, null, null, null, null, null, null);
    }

    public static Number real(BigDecimal value) {
        return new Number(Type.BIGFLOAT, 0, null, 0.0, value, 0, 0, null, null, null, null, null, null);
    }

    public static Number complex(Number real, Number i) {
        return new Number(Type.COMPLEX, 0, null, 0.0, null, 0, 0, null, null, real, i, null, null);
    }

    public static Number quaternion(Number real, Number i, Number j, Number k) {
        return new Number(Type.QUATERNION, 0, null, 0.0, null, 0, 0, null, null, real, i, j, k);
    }


    // ------ Generic zeros and ones ------
    public static Number zero(Type type) {
        return switch (type) {
            case INT          -> ZERO_INT;
            case BIGINT       -> ZERO_BIGINT;
            case FLOAT        -> ZERO_FLOAT;
            case BIGFLOAT     -> ZERO_BIGFLOAT;
            case RATIONAL     -> ZERO_RATIONAL;
            case BIGRATIONAL  -> ZERO_BIGRATIONAL;
            case COMPLEX      -> ZERO_COMPLEX;
            case QUATERNION   -> ZERO_QUATERNION;
        };
    }

    private static String formatComplex(Number z) {
        Number realPart = (z.real != null) ? z.real : ZERO_INT;
        Number imagPart = (z.ipart != null) ? z.ipart : ZERO_INT;
        if (isZero(imagPart)) {
            return realPart.toString();
        }
        boolean positive = greaterThan(imagPart, zero(imagPart));
        Number magnitude = positive ? imagPart : negate(imagPart);
        StringBuilder sb = new StringBuilder();
        sb.append(realPart);
        sb.append(positive ? "+" : "-");
        if (!numericEquals(magnitude, one(magnitude))) {
            sb.append(magnitude);
        }
        sb.append("i");
        return sb.toString();
    }

    private static String formatQuaternion(Number q) {
        Number realPart = (q.real != null) ? q.real : ZERO_INT;
        Number iPart = (q.ipart != null) ? q.ipart : ZERO_INT;
        Number jPart = (q.jpart != null) ? q.jpart : ZERO_INT;
        Number kPart = (q.kpart != null) ? q.kpart : ZERO_INT;

        StringBuilder sb = new StringBuilder();
        sb.append(realPart);
        sb.append(formatQuaternionComponent(iPart, "i"));
        sb.append(formatQuaternionComponent(jPart, "j"));
        sb.append(formatQuaternionComponent(kPart, "k"));
        return sb.toString();
    }

    private static String formatQuaternionComponent(Number component, String suffix) {
        Number value = component;
        if (isZero(value)) {
            return "+0" + suffix;
        }
        boolean positive = greaterThan(value, zero(value));
        Number magnitude = positive ? value : negate(value);
        StringBuilder sb = new StringBuilder();
        sb.append(positive ? "+" : "-");
        if (!numericEquals(magnitude, one(magnitude))) {
            sb.append(magnitude);
        }
        sb.append(suffix);
        return sb.toString();
    }

    public static Number zero(Number likeThis) {
        return zero(likeThis.type);
    }

    public static Number one(Type type) {
        return switch (type) {
            case INT          -> ONE_INT;
            case BIGINT       -> ONE_BIGINT;
            case FLOAT        -> ONE_FLOAT;
            case BIGFLOAT     -> ONE_BIGFLOAT;
            case RATIONAL     -> ONE_RATIONAL;
            case BIGRATIONAL  -> ONE_BIGRATIONAL;
            case COMPLEX      -> ONE_COMPLEX;
            case QUATERNION   -> ONE_QUATERNION;
        };
    }

    public static Number one(Number likeThis) {
        return one(likeThis.type);
    }



    // ---- gcd helper ----
    private static long gcd(long a, long b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            long temp = b;
            b = a % b;
            a = temp;
        }
        return a == 0 ? 1 : a;
    }


    // Convert any scalar Number (not COMPLEX/QUATERNION) to BigDecimal.
    private static BigDecimal toBigDecimal(Number n) {
        switch (n.type) {
            case BIGFLOAT:   return n.bigFloatVal;
            case FLOAT:      return BigDecimal.valueOf(n.floatVal);
            case BIGINT:     return new BigDecimal(n.bigVal);
            case INT:        return BigDecimal.valueOf(n.intVal);
            case BIGRATIONAL:return new BigDecimal(n.bigNum)
                                .divide(new BigDecimal(n.bigDen), MathContext.DECIMAL128);
            case RATIONAL:   return BigDecimal.valueOf(n.num)
                                .divide(BigDecimal.valueOf(n.den), MathContext.DECIMAL128);
            default:
                throw new IllegalArgumentException("toBigDecimal: non-scalar type " + n.type);
        }
    }

    private static double toDouble(Number n) {
        return switch (n.type) {
            case INT          -> (double) n.intVal;
            case BIGINT       -> n.bigVal.doubleValue();
            case FLOAT        -> n.floatVal;
            case BIGFLOAT     -> n.bigFloatVal.doubleValue();
            case RATIONAL     -> (double) n.num / (double) n.den;
            case BIGRATIONAL  -> n.bigNum.doubleValue() / n.bigDen.doubleValue();
            default -> throw new IllegalArgumentException("toDouble: non-scalar type " + n.type);
        };
    }

    private static boolean isScalar(Number n) {
        return n.type != Type.COMPLEX && n.type != Type.QUATERNION;
    }

    private static boolean isZero(Number n) {
        return switch (n.type) {
            case INT          -> n.intVal == 0;
            case BIGINT       -> n.bigVal.signum() == 0;
            case FLOAT        -> n.floatVal == 0.0;
            case BIGFLOAT     -> n.bigFloatVal.compareTo(BigDecimal.ZERO) == 0;
            case RATIONAL     -> n.num == 0;
            case BIGRATIONAL  -> n.bigNum.signum() == 0;
            case COMPLEX      -> isZero(n.real != null ? n.real : ZERO_INT)
                               && isZero(n.ipart != null ? n.ipart : ZERO_INT);
            case QUATERNION   -> isZero(n.real != null ? n.real : ZERO_INT)
                               && isZero(n.ipart != null ? n.ipart : ZERO_INT)
                               && isZero(n.jpart != null ? n.jpart : ZERO_INT)
                               && isZero(n.kpart != null ? n.kpart : ZERO_INT);
        };
    }

    private static BigInteger[] toBigFraction(Number n) {
        BigInteger num;
        BigInteger den;
        switch (n.type) {
            case INT: {
                num = BigInteger.valueOf(n.intVal);
                den = BigInteger.ONE;
                break;
            }
            case BIGINT: {
                num = n.bigVal;
                den = BigInteger.ONE;
                break;
            }
            case RATIONAL: {
                num = BigInteger.valueOf(n.num);
                den = BigInteger.valueOf(n.den);
                break;
            }
            case BIGRATIONAL: {
                num = n.bigNum;
                den = n.bigDen;
                break;
            }
            default:
                throw new IllegalArgumentException("toBigFraction: requires exact scalar type, got " + n.type);
        }
        if (den.signum() < 0) {
            num = num.negate();
            den = den.negate();
        }
        return new BigInteger[]{num, den};
    }

    private static Number fromFraction(BigInteger num, BigInteger den) {
        if (den.equals(BigInteger.ZERO))
            throw new ArithmeticException("Division by zero in fraction normalization");
        if (den.equals(BigInteger.ONE)) {
            try {
                return Number.integer(num.longValueExact());
            } catch (ArithmeticException e) {
                return Number.integer(num);
            }
        }
        try {
            long n = num.longValueExact();
            long d = den.longValueExact();
            return Number.rational(n, d);
        } catch (ArithmeticException e) {
            return Number.rational(num, den);
        }
    }

    private static BigInteger floorDiv(BigInteger num, BigInteger den) {
        if (den.signum() == 0) throw new ArithmeticException("Division by zero");
        BigInteger[] qr = num.divideAndRemainder(den);
        BigInteger quotient = qr[0];
        BigInteger remainder = qr[1];
        if (remainder.signum() != 0 && ((den.signum() < 0) != (remainder.signum() < 0))) {
            quotient = quotient.subtract(BigInteger.ONE);
        }
        return quotient;
    }

    private static BigInteger toBigIntegerExact(Number n) {
        return switch (n.type) {
            case INT -> BigInteger.valueOf(n.intVal);
            case BIGINT -> n.bigVal;
            case RATIONAL -> (n.den == 1) ? BigInteger.valueOf(n.num) : null;
            case BIGRATIONAL -> n.bigDen.equals(BigInteger.ONE) ? n.bigNum : null;
            case FLOAT -> {
                double value = n.floatVal;
                if (!Double.isFinite(value)) yield null;
                BigDecimal bd = BigDecimal.valueOf(value).stripTrailingZeros();
                yield bd.scale() <= 0 ? bd.toBigIntegerExact() : null;
            }
            case BIGFLOAT -> {
                BigDecimal bd = n.bigFloatVal.stripTrailingZeros();
                try {
                    yield bd.scale() <= 0 ? bd.toBigIntegerExact() : null;
                } catch (ArithmeticException ex) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    private static Number negate(Number n) {
        switch (n.type) {
            case INT:
                return Number.integer(-n.intVal);
            case BIGINT:
                return Number.integer(n.bigVal.negate());
            case FLOAT:
                return Number.real(-n.floatVal);
            case BIGFLOAT:
                return Number.real(n.bigFloatVal.negate());
            case RATIONAL:
                return Number.rational(-n.num, n.den);
            case BIGRATIONAL:
                return Number.rational(n.bigNum.negate(), n.bigDen);
            case COMPLEX: {
                Number realneg  = negate(n.real != null  ? n.real  : Number.integer(0));
                Number ipartneg = negate(n.ipart != null ? n.ipart : Number.integer(0));
                return Number.complex(realneg, ipartneg);
            }
            case QUATERNION: {
                Number rneg = negate(n.real  != null ? n.real  : Number.integer(0));
                Number ineg = negate(n.ipart != null ? n.ipart : Number.integer(0));
                Number jneg = negate(n.jpart != null ? n.jpart : Number.integer(0));
                Number kneg = negate(n.kpart != null ? n.kpart : Number.integer(0));
                return Number.quaternion(rneg, ineg, jneg, kneg);
            }
            default:
                throw new IllegalStateException("unknown numeric type: " + n.type);
        }
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
                // If RHS is a quaternion, promote the LHS complex to a quaternion and add there.
                if (b.type == Type.QUATERNION) {
                    Number qa = Number.quaternion(a.real, a.ipart, Number.integer(0), Number.integer(0));
                    return addQuaternion(qa, b);
                }
                // Otherwise, keep your existing Complex promotion
                if (b.type != Type.COMPLEX)
                    b = Number.complex(b, Number.integer(0));
                return addComplex(a, b);
            case QUATERNION:
                if (b.type != Type.QUATERNION) {
                    if (b.type == Type.COMPLEX) {
                        b = Number.quaternion(b.real, b.ipart, Number.integer(0), Number.integer(0));
                    } else {
                        b = Number.quaternion(b, Number.integer(0), Number.integer(0), Number.integer(0));
                    }
                }
                return addQuaternion(a, b);
            default:
                throw new IllegalStateException("Unknown type: " + a.type);
        }
    }
    public static Number sub(Number a, Number b){
        return add(a, negate(b));
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
        final double right;
        switch (b.type) {
            case FLOAT:   right = b.floatVal; break;
            case INT:     right = b.intVal;   break;
            case BIGINT:  right = b.bigVal.doubleValue(); break;
            default:      return addBigFloat(Number.real(BigDecimal.valueOf(left)), b);
        }
        double sum = left + right;
        return Double.isFinite(sum) ? Number.real(sum)
                                    : addBigFloat(Number.real(BigDecimal.valueOf(left)), b);
    }

    private static Number addBigFloat(Number a, Number b) {
        BigDecimal left  = toBigDecimal(a);
        BigDecimal right = toBigDecimal(b);
        return Number.real(left.add(right));
    }

    private static Number addBigRational(Number a, Number b) {
        BigInteger num = a.bigNum.multiply(b.bigDen).add(b.bigNum.multiply(a.bigDen));
        BigInteger den = a.bigDen.multiply(b.bigDen);
        return Number.rational(num, den);
    }

    private static Number addComplex(Number a, Number b) {
        Number aReal = (a.real != null) ? a.real : Number.integer(0);
        Number aipart = (a.ipart != null) ? a.ipart : Number.integer(0);
        Number bReal = (b.real != null) ? b.real : Number.integer(0);
        Number bipart = (b.ipart != null) ? b.ipart : Number.integer(0);

        Number realPart = add(aReal, bReal);
        Number ipartPart = add(aipart, bipart);
        return Number.complex(realPart, ipartPart);
    }


    private static Number addQuaternion(Number a, Number b) {
        Number ar = (a.real  != null) ? a.real  : Number.integer(0);
        Number ai = (a.ipart != null) ? a.ipart : Number.integer(0);
        Number aj = (a.jpart != null) ? a.jpart : Number.integer(0);
        Number ak = (a.kpart != null) ? a.kpart : Number.integer(0);

        Number br = (b.real  != null) ? b.real  : Number.integer(0);
        Number bi = (b.ipart != null) ? b.ipart : Number.integer(0);
        Number bj = (b.jpart != null) ? b.jpart : Number.integer(0);
        Number bk = (b.kpart != null) ? b.kpart : Number.integer(0);

        return Number.quaternion(
            add(ar, br),
            add(ai, bi),
            add(aj, bj),
            add(ak, bk)
        );
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
                            return Number.rational(numBig, denBig);
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
                            return Number.rational(numBig, aden);
                        }
                    }

                    case BIGINT: {
                        BigInteger anum = BigInteger.valueOf(a.num);
                        BigInteger aden = BigInteger.valueOf(a.den);
                        BigInteger bnum = b.bigVal;
                        BigInteger numBig = anum.add(bnum.multiply(aden));
                        return Number.rational(numBig, aden);
                    }

                    case BIGRATIONAL: {
                        BigInteger anum = BigInteger.valueOf(a.num);
                        BigInteger aden = BigInteger.valueOf(a.den);
                        BigInteger numBig = anum.multiply(b.bigDen)
                            .add(b.bigNum.multiply(aden));
                        BigInteger denBig = aden.multiply(b.bigDen);
                        return Number.rational(numBig, denBig);
                    }

                    case FLOAT: {
                        double left = ((double) a.num / (double) a.den);
                        double sum  = left + b.floatVal;
                        if (!Double.isFinite(sum)) {
                            // Promote to BigDecimal precision arithmetic
                            BigDecimal leftBD = BigDecimal.valueOf(a.num)
                                .divide(BigDecimal.valueOf(a.den), MathContext.DECIMAL128);
                            BigDecimal rightBD = BigDecimal.valueOf(b.floatVal);
                            BigDecimal sumBD = leftBD.add(rightBD);
                            return Number.real(sumBD);
                        }
                        return Number.real(sum);
                    }

                    case BIGFLOAT: {
                        BigDecimal left = BigDecimal.valueOf(a.num)
                            .divide(BigDecimal.valueOf(a.den), MathContext.DECIMAL128);
                        BigDecimal sum = left.add(b.bigFloatVal);
                        return Number.real(sum);
                    }

                    case COMPLEX: {
                        Number realPart = addRational(a, b.real);
                        return Number.complex(realPart, b.ipart);
                    }

                    case QUATERNION: {
                        // Promote rational to quaternion (a + 0i + 0j + 0k)
                        Number realPart = addRational(a, b.real);
                        return Number.quaternion(realPart, b.ipart, b.jpart, b.kpart);
                    }

                    default:
                        throw new IllegalArgumentException("Unsupported type combination: RATIONAL + " + b.type);
                }
            }

            default:
                throw new IllegalArgumentException("Invalid call: addRational with non-rational lhs " + a.type);
        }
    } 
    //------ Multiplication -------

    //------ Multiplication -------
    public static Number multiply(Number a, Number b) {
        // Preserve operand order for non-commutative pairs: COMPLEX ↔ QUATERNION.
        boolean nonCommutativePair =
            (a.type == Type.QUATERNION || b.type == Type.QUATERNION) &&
            (a.type == Type.COMPLEX    || b.type == Type.COMPLEX);

        if (!nonCommutativePair && a.type.ordinal() < b.type.ordinal()) {
            return multiply(b, a);
        }

        switch (a.type) {
            case INT:         return multiplyInt(a, b);
            case BIGINT:      return multiplyBigInt(a, b);
            case RATIONAL:    return multiplyRational(a, b);
            case BIGRATIONAL: return multiplyBigRational(a, b);
            case FLOAT:       return multiplyFloat(a, b);
            case BIGFLOAT:    return multiplyBigFloat(a, b);
            case COMPLEX:
                // Preserve order for non-commutative C×Q
                if (b.type == Type.QUATERNION) {
                    Number qa = Number.quaternion(a.real, a.ipart, Number.integer(0), Number.integer(0));
                    return multiplyQuaternion(qa, b);
                }
                if (b.type != Type.COMPLEX)
                    b = Number.complex(b, Number.integer(0));
                return multiplyComplex(a, b);
            case QUATERNION:
                if (b.type != Type.QUATERNION) {
                    if (b.type == Type.COMPLEX) {
                        b = Number.quaternion(b.real, b.ipart, Number.integer(0), Number.integer(0));
                    } else {
                        b = Number.quaternion(b, Number.integer(0), Number.integer(0), Number.integer(0));
                    }
                }
                return multiplyQuaternion(a, b);
            default:
                throw new IllegalStateException("Unknown type: " + a.type);
        }
    }

    

    private static Number multiplyInt(Number a, Number b) {
        try{
            long product = Math.multiplyExact(a.intVal,b.intVal);
            return Number.integer(product);
        }
        catch (ArithmeticException e) {
            BigInteger bigProduct = BigInteger.valueOf(a.intVal).multiply(BigInteger.valueOf(b.intVal));
            return Number.integer(bigProduct);
        }
    }

    private static Number multiplyBigInt(Number a, Number b) {
        BigInteger left = (a.type == Type.BIGINT) ? a.bigVal : BigInteger.valueOf(a.intVal);
        BigInteger right = (b.type == Type.BIGINT) ? b.bigVal : BigInteger.valueOf(b.intVal);
        return Number.integer(left.multiply(right));
    }
    
    private static Number multiplyFloat(Number a, Number b) {
        double left = a.floatVal;
        final double right;
        switch (b.type) {
            case FLOAT:   right = b.floatVal; break;
            case INT:     right = b.intVal;   break;
            case BIGINT:  right = b.bigVal.doubleValue(); break;
            default:      return multiplyBigFloat(Number.real(BigDecimal.valueOf(left)), b);
        }
        double prod = left * right;
        return Double.isFinite(prod) ? Number.real(prod)
                                    : multiplyBigFloat(Number.real(BigDecimal.valueOf(left)), b);
    }

    private static Number multiplyBigFloat(Number a, Number b) {
        BigDecimal left  = toBigDecimal(a);
        BigDecimal right = toBigDecimal(b);
        return Number.real(left.multiply(right));
    }

    private static Number multiplyQuaternion(Number a, Number b) {

        if (b.type == Type.COMPLEX) {
            Number qb = Number.quaternion(b.real, b.ipart, Number.integer(0), Number.integer(0));
            return multiplyQuaternion(a, qb);
        }
        // a = ar + ai*i + aj*j + ak*k
        // b = br + bi*i + bj*j + bk*k
        Number ar = (a.real  != null) ? a.real  : Number.integer(0);
        Number ai = (a.ipart != null) ? a.ipart : Number.integer(0);
        Number aj = (a.jpart != null) ? a.jpart : Number.integer(0);
        Number ak = (a.kpart != null) ? a.kpart : Number.integer(0);

        Number br = (b.real  != null) ? b.real  : Number.integer(0);
        Number bi = (b.ipart != null) ? b.ipart : Number.integer(0);
        Number bj = (b.jpart != null) ? b.jpart : Number.integer(0);
        Number bk = (b.kpart != null) ? b.kpart : Number.integer(0);

        // Hamilton product:
        // real: ar*br - ai*bi - aj*bj - ak*bk
        Number real = sub(sub(sub(multiply(ar, br),
                                multiply(ai, bi)),
                            multiply(aj, bj)),
                        multiply(ak, bk));

        // i: ar*bi + ai*br + aj*bk - ak*bj
        Number i = sub(add(add(multiply(ar, bi),
                            multiply(ai, br)),
                        multiply(aj, bk)),
                    multiply(ak, bj));

        // j: ar*bj - ai*bk + aj*br + ak*bi
        Number j = add(add(sub(multiply(ar, bj),
                            multiply(ai, bk)),
                        multiply(aj, br)),
                    multiply(ak, bi));

        // k: ar*bk + ai*bj - aj*bi + ak*br
        Number k = add(sub(add(multiply(ar, bk),
                            multiply(ai, bj)),
                        multiply(aj, bi)),
                    multiply(ak, br));

        return Number.quaternion(real, i, j, k);
    }

    private static Number multiplyRational(Number a, Number b) {
        long numerator = 0;
        long denominator = 0;
        switch (a.type) {
            case RATIONAL: {
                switch (b.type) {
                    case RATIONAL: {
                        try {
                            numerator = Math.multiplyExact(a.num,b.num);
                            denominator = Math.multiplyExact(a.den,b.den);
                            return Number.rational(numerator,denominator);
                        } catch (ArithmeticException e) {
                            BigInteger numBig = BigInteger.valueOf(a.num).multiply(BigInteger.valueOf(b.num));
                            BigInteger denBig = BigInteger.valueOf(a.den).multiply(BigInteger.valueOf(b.den));
                            return Number.rational(numBig,denBig);
                        }
                    }

                    case INT: {
                        try {
                            numerator = Math.multiplyExact(a.num,b.intVal);
                            return Number.rational(numerator,a.den);
                        } catch (ArithmeticException e) {
                            BigInteger numBig = BigInteger.valueOf(a.num).multiply(BigInteger.valueOf(b.intVal));
                            BigInteger denBig = BigInteger.valueOf(a.den);
                            return Number.rational(numBig,denBig);
                        }
                    }
                    case BIGINT: {
                        BigInteger numBig = BigInteger.valueOf(a.num).multiply(b.bigVal);
                        BigInteger denBig = BigInteger.valueOf(a.den);
                        return Number.rational(numBig,denBig);
                    }

                    case BIGRATIONAL: {
                        BigInteger numBig = BigInteger.valueOf(a.num).multiply(b.bigNum);
                        BigInteger denBig = BigInteger.valueOf(a.den).multiply(b.bigDen);
                        return Number.rational(numBig,denBig);
                    }

                    case FLOAT: {
                        double left = ((double) a.num / (double) a.den);
                        double product  = left*b.floatVal;
                        if (!Double.isFinite(product)) {
                            // Promote to BigDecimal precision arithmetic
                            BigDecimal leftBD = BigDecimal.valueOf(a.num).divide(BigDecimal.valueOf(a.den), MathContext.DECIMAL128);
                            BigDecimal rightBD = BigDecimal.valueOf(b.floatVal);
                            BigDecimal prodBD = leftBD.multiply(rightBD);
                            return Number.real(prodBD);
                        }
                        return Number.real(product);
                    }

                    case BIGFLOAT: {
                        BigDecimal left = BigDecimal.valueOf(a.num).divide(BigDecimal.valueOf(a.den), MathContext.DECIMAL128);
                        BigDecimal product = left.multiply(b.bigFloatVal);
                        return Number.real(product);
                    }

                    case COMPLEX: {
                        Number real = multiplyRational(a,b.real);
                        Number ipart = multiplyRational(a,b.ipart);
                        return Number.complex(real,ipart);
                    }

                    case QUATERNION: {
                        // Scalar multiplication — multiply each component by the rational
                        Number r = multiplyRational(a, b.real);
                        Number i = multiplyRational(a, b.ipart);
                        Number j = multiplyRational(a, b.jpart);
                        Number k = multiplyRational(a, b.kpart);
                        return Number.quaternion(r, i, j, k);
                    }
                    default:
                        throw new IllegalArgumentException("Unsupported type combination: RATIONAL + " + b.type);
                }
            }

            default:
                throw new IllegalArgumentException("Invalid call: addRational with non-rational lhs " + a.type);
        }

    }
    private static Number multiplyBigRational(Number a, Number b){
        BigInteger numerator   = a.bigNum.multiply(b.bigNum);
        BigInteger denominator = a.bigDen.multiply(b.bigDen);
        return Number.rational(numerator,denominator);
    }

    private static Number multiplyComplex(Number a, Number b){
        if (b.type == Type.QUATERNION) {
            // promote a to quaternion for LEFT operand (not symmetric!)
            Number qa = Number.quaternion(a.real, a.ipart, Number.integer(0), Number.integer(0));
            return multiplyQuaternion(qa, b); // correct
        }
        Number real = add(multiply(a.real,b.real),negate(multiply(a.ipart,b.ipart)));
        Number ipart = add(multiply(a.real,b.ipart),multiply(a.ipart,b.real));
        return Number.complex(real,ipart);
    }
    //------ Division -------
    public static Number divide(Number a, Number b) { 
        switch (a.type) {
            case INT:         return divideInt(a, b);
            case BIGINT:      return divideBigInt(a, b);
            case RATIONAL:    return divideRational(a, b);
            case BIGRATIONAL: return divideBigRational(a, b);
            case FLOAT:       return divideFloat(a, b);
            case BIGFLOAT:    return divideBigFloat(a, b);
            case COMPLEX:
                if (b.type == Type.QUATERNION) {
                    Number qa = Number.quaternion(a.real, a.ipart, Number.integer(0), Number.integer(0));
                    return divideQuaternion(qa, b);
                }
                if (b.type != Type.COMPLEX)
                    b = Number.complex(b, Number.integer(0));
                return divideComplex(a, b);
            case QUATERNION:
                if (b.type != Type.QUATERNION) {
                    if (b.type == Type.COMPLEX) {
                        b = Number.quaternion(b.real, b.ipart, Number.integer(0), Number.integer(0));
                    } else {
                        b = Number.quaternion(b, Number.integer(0), Number.integer(0), Number.integer(0));
                    }
                }
                return divideQuaternion(a, b);
            default:
                throw new IllegalStateException("Unknown type: " + a.type);
        }

    } 
    

    private static Number divideInt(Number a, Number b) {
        switch (b.type) {
            case INT: {
                if (b.intVal == 0) throw new ArithmeticException("Division by zero");
                if (a.intVal % b.intVal == 0) return Number.integer(a.intVal / b.intVal);
                return Number.rational(a.intVal, b.intVal);
            }
            case BIGINT: {
                if (b.bigVal.signum() == 0) throw new ArithmeticException("Division by zero");
                return Number.rational(BigInteger.valueOf(a.intVal), b.bigVal);
            }
            case RATIONAL: {
                if (b.num == 0) throw new ArithmeticException("Division by zero");
                // a / (p/q) = a * (q/p)
                try {
                    long num = Math.multiplyExact(a.intVal, b.den);
                    return Number.rational(num, b.num);
                } catch (ArithmeticException e) {
                    BigInteger num = BigInteger.valueOf(a.intVal).multiply(BigInteger.valueOf(b.den));
                    BigInteger den = BigInteger.valueOf(b.num);
                    return Number.rational(num, den);
                }
            }
            case BIGRATIONAL: {
                if (b.bigNum.signum() == 0) throw new ArithmeticException("Division by zero");
                BigInteger num = BigInteger.valueOf(a.intVal).multiply(b.bigDen);
                BigInteger den = b.bigNum;
                return Number.rational(num, den);
            }
            case FLOAT: {
                return Number.real(a.intVal / b.floatVal);
            }
            case BIGFLOAT: {
                return Number.real(BigDecimal.valueOf(a.intVal)
                        .divide(b.bigFloatVal, MathContext.DECIMAL128));
            }
            case COMPLEX: {
                // treat a as a + 0i
                return divideComplex(Number.complex(a, Number.integer(0)), b);
            }
            default:
                throw new IllegalStateException("Unsupported divisor type: " + b.type);
        }
    }

    private static Number divideBigInt(Number a, Number b) {
        BigInteger bigA = (a.type == Type.BIGINT) ? a.bigVal : BigInteger.valueOf(a.intVal);
        BigInteger bigB = (b.type == Type.BIGINT) ? b.bigVal : BigInteger.valueOf(b.intVal);

        if (bigB.signum() == 0) throw new ArithmeticException("Division by zero");
        if (bigA.signum() == 0) return Number.integer(BigInteger.ZERO);

        BigInteger[] qr = bigA.divideAndRemainder(bigB);
        if (qr[1].signum() == 0) {
            return Number.integer(qr[0]); // exact BigInteger quotient
        }
        return Number.rational(bigA, bigB); // factory will reduce & normalize sign
    }
    

    private static Number divideFloat(Number a, Number b) {
        double left = a.floatVal;

        switch (b.type) {
            case FLOAT: {
                double q = left / b.floatVal;
                if (Double.isFinite(q)) return Number.real(q);
                return Number.real(BigDecimal.valueOf(left)
                        .divide(BigDecimal.valueOf(b.floatVal), MathContext.DECIMAL128));
            }
            case INT: {
                double q = left / b.intVal;
                if (Double.isFinite(q)) return Number.real(q);
                return Number.real(BigDecimal.valueOf(left)
                        .divide(BigDecimal.valueOf(b.intVal), MathContext.DECIMAL128));
            }
            case BIGINT: {
                return Number.real(BigDecimal.valueOf(left)
                        .divide(new BigDecimal(b.bigVal), MathContext.DECIMAL128));
            }
            case RATIONAL: {
                // left / (p/q) = left * (q/p)
                double right = ((double) b.num) / ((double) b.den);
                double q = left / right;
                if (Double.isFinite(q)) return Number.real(q);
                BigDecimal num = BigDecimal.valueOf(left).multiply(BigDecimal.valueOf(b.den));
                return Number.real(num.divide(BigDecimal.valueOf(b.num), MathContext.DECIMAL128));
            }
            case BIGRATIONAL: {
                // left * (bigDen / bigNum)
                BigDecimal num = BigDecimal.valueOf(left).multiply(new BigDecimal(b.bigDen));
                return Number.real(num.divide(new BigDecimal(b.bigNum), MathContext.DECIMAL128));
            }
            case BIGFLOAT: {
                return Number.real(BigDecimal.valueOf(left)
                        .divide(b.bigFloatVal, MathContext.DECIMAL128));
            }
            case COMPLEX: {
                return divideComplex(Number.complex(Number.real(left), Number.integer(0)), b);
            }
            default:
                throw new IllegalStateException("Unsupported divisor type for FLOAT / " + b.type);
        }
    }
    
    private static Number divideBigFloat(Number a, Number b) {
        BigDecimal left  = toBigDecimal(a);
        BigDecimal right = toBigDecimal(b);
        BigDecimal q = left.divide(right, MathContext.DECIMAL128);
        return Number.real(q);
    }


    private static Number divideRational(Number a, Number b) {
        // b must not be zero
        if ((b.type == Type.RATIONAL && b.num == 0) ||
            (b.type == Type.INT && b.intVal == 0))
            throw new ArithmeticException("Division by zero");

        // Reciprocal: swap numerator and denominator of b
        Number reciprocal;
        switch (b.type) {
            case RATIONAL:
                reciprocal = Number.rational(b.den, b.num);
                break;
            case BIGRATIONAL:
                reciprocal = Number.rational(b.bigDen, b.bigNum);
                break;
            case INT:
                reciprocal = Number.rational(1, b.intVal);
                break;
            case BIGINT:
                reciprocal = Number.rational(BigInteger.ONE, b.bigVal);
                break;
            case FLOAT:
                reciprocal = Number.real(1.0 / b.floatVal);
                break;
            case BIGFLOAT:
                reciprocal = Number.real(BigDecimal.ONE.divide(b.bigFloatVal, MathContext.DECIMAL128));
                break;
            case COMPLEX:
                reciprocal = reciprocalComplex(b);
                break;
            case QUATERNION:
                reciprocal = reciprocalQuaternion(b);
                break;
            default:
                throw new IllegalArgumentException("Unsupported type: " + b.type);
        }
        return multiplyRational(a, reciprocal);
    }
    private static Number divideBigRational(Number a, Number b){
        Number reciprocal = Number.rational(b.bigDen,b.bigNum);  
        return multiplyBigRational(a,reciprocal);
    }

    private static Number divideComplex(Number a, Number b){
        Number denominator = add(multiply(b.real,b.real),multiply(b.ipart,b.ipart));
        Number rnumerator  = add(multiply(a.real,b.real),multiply(a.ipart,b.ipart));
        Number inumerator  = sub(multiply(a.ipart,b.real),multiply(a.real,b.ipart));        
        return Number.complex(divide(rnumerator, denominator),divide(inumerator, denominator));
    }

    private static Number reciprocalComplex(Number z) { 
        Number a = z.real;
        Number b = z.ipart; 
        Number a2 = multiply(a, a);
        Number b2 = multiply(b, b);
        Number denom = add(a2, b2);
        Number negB = negate(b);
        Number conj = Number.complex(a, negB);
        return divide(conj, denom);
    }

    private static Number divideQuaternion(Number a, Number b) {
        Number invB = reciprocalQuaternion(b);
        return multiplyQuaternion(a, invB);
    }

    private static Number reciprocalQuaternion(Number q) {
        Number ar = (q.real  != null) ? q.real  : Number.integer(0);
        Number ai = (q.ipart != null) ? q.ipart : Number.integer(0);
        Number aj = (q.jpart != null) ? q.jpart : Number.integer(0);
        Number ak = (q.kpart != null) ? q.kpart : Number.integer(0);

        // |q|^2 = ar^2 + ai^2 + aj^2 + ak^2
        Number n2 = add(add(multiply(ar, ar), multiply(ai, ai)),
                        add(multiply(aj, aj), multiply(ak, ak)));
 
        // Conjugate(q) = (ar, -ai, -aj, -ak)
        Number cr = ar;
        Number ci = negate(ai);
        Number cj = negate(aj);
        Number ck = negate(ak);

        // q^{-1} = Conj(q) / |q|^2  (component-wise division by scalar n2)
        return Number.quaternion(
            divide(cr, n2),
            divide(ci, n2),
            divide(cj, n2),
            divide(ck, n2)
        );
    }

    //------ Modulus -------
    public static Number mod(Number a, Number b) {
        if (b.type == Type.COMPLEX || b.type == Type.QUATERNION)
            throw new IllegalArgumentException("Modulo divisor must be scalar; got " + b.type);
        if (isZero(b))
            throw new ArithmeticException("Modulo by zero");

        if (a.type == Type.QUATERNION) return modQuaternion(a, b);
        if (a.type == Type.COMPLEX)    return modComplex(a, b);

        if (!isScalar(a))
            throw new IllegalArgumentException("Unsupported modulo type: " + a.type);

        if (a.type == Type.BIGFLOAT || b.type == Type.BIGFLOAT)
            return modBigFloat(a, b);
        if (a.type == Type.FLOAT || b.type == Type.FLOAT)
            return modFloat(a, b);

        return modExact(a, b);
    }

    private static Number modExact(Number a, Number b) {
        BigInteger[] fracA = toBigFraction(a);
        BigInteger[] fracB = toBigFraction(b);
        BigInteger na = fracA[0];
        BigInteger da = fracA[1];
        BigInteger nb = fracB[0];
        BigInteger db = fracB[1];

        if (nb.signum() == 0)
            throw new ArithmeticException("Modulo by zero");

        BigInteger quotient = floorDiv(na.multiply(db), da.multiply(nb));
        BigInteger remainderNum = na.multiply(db).subtract(quotient.multiply(nb).multiply(da));
        BigInteger remainderDen = da.multiply(db);

        return fromFraction(remainderNum, remainderDen);
    }

    private static Number modFloat(Number a, Number b) {
        double left = toDouble(a);
        double right = toDouble(b);
        if (!Double.isFinite(left) || !Double.isFinite(right))
            return modBigFloat(a, b);
        if (right == 0.0)
            throw new ArithmeticException("Modulo by zero");
        double quotient = Math.floor(left / right);
        double remainder = left - right * quotient;
        if (!Double.isFinite(quotient) || !Double.isFinite(remainder))
            return modBigFloat(a, b);
        return Number.real(remainder);
    }

    private static Number modBigFloat(Number a, Number b) {
        BigDecimal left = toBigDecimal(a);
        BigDecimal right = toBigDecimal(b);
        if (right.compareTo(BigDecimal.ZERO) == 0)
            throw new ArithmeticException("Modulo by zero");
        BigDecimal quotient = left.divide(right, MathContext.DECIMAL128);
        BigDecimal floored = quotient.setScale(0, RoundingMode.FLOOR);
        BigDecimal remainder = left.subtract(right.multiply(floored));
        return Number.real(remainder);
    }

    private static Number modComplex(Number a, Number b) {
        if (b.type == Type.COMPLEX || b.type == Type.QUATERNION)
            throw new IllegalArgumentException("Modulo divisor for complex values must be scalar");
        Number realPart = mod(a.real != null ? a.real : ZERO_INT, b);
        Number imagPart = mod(a.ipart != null ? a.ipart : ZERO_INT, b);
        return Number.complex(realPart, imagPart);
    }

    private static Number modQuaternion(Number a, Number b) {
        if (b.type == Type.COMPLEX || b.type == Type.QUATERNION)
            throw new IllegalArgumentException("Modulo divisor for quaternion values must be scalar");
        Number realPart = mod(a.real != null ? a.real : ZERO_INT, b);
        Number ipartPart = mod(a.ipart != null ? a.ipart : ZERO_INT, b);
        Number jpartPart = mod(a.jpart != null ? a.jpart : ZERO_INT, b);
        Number kpartPart = mod(a.kpart != null ? a.kpart : ZERO_INT, b);
        return Number.quaternion(realPart, ipartPart, jpartPart, kpartPart);
    }


    //------ Power -------
    public static Number pow(Number base, Number exponent) {
        if (!isScalar(exponent))
            throw new IllegalArgumentException("Exponent must be scalar; got " + exponent.type);

        BigInteger expInteger = toBigIntegerExact(exponent);
        if (expInteger != null) {
            return powInteger(base, expInteger);
        }

        if (!isScalar(base))
            throw new IllegalArgumentException("Non-integer exponents are unsupported for type " + base.type);

        double baseValue = toDouble(base);
        double expValue = toDouble(exponent);
        if (!Double.isFinite(baseValue) || !Double.isFinite(expValue))
            throw new ArithmeticException("Exponentiation out of range");
        double result = Math.pow(baseValue, expValue);
        if (!Double.isFinite(result) || Double.isNaN(result))
            throw new ArithmeticException("Exponentiation result undefined for given operands");
        return Number.real(result);
    }

    private static Number powInteger(Number base, BigInteger exponent) {
        if (exponent.signum() == 0) {
            return Number.one(base);
        }

        boolean negative = exponent.signum() < 0;
        BigInteger power = exponent.abs();

        Number result = Number.one(base);
        Number factor = base;

        while (power.signum() > 0) {
            if (power.testBit(0)) {
                result = multiply(result, factor);
            }
            power = power.shiftRight(1);
            if (power.signum() > 0) {
                factor = multiply(factor, factor);
            }
        }

        if (negative) {
            return divide(Number.one(base), result);
        }
        return result;
    }


    //------ Conjugates & Inverse -------
    public static Number complexConjugate(Number z) {
        if (z.type != Type.COMPLEX)
            throw new IllegalArgumentException("complexConjugate expects COMPLEX, got " + z.type);
        Number realPart = (z.real != null) ? z.real : ZERO_INT;
        Number imagPart = (z.ipart != null) ? z.ipart : ZERO_INT;
        return Number.complex(realPart, negate(imagPart));
    }

    public static Number quaternionConjugate(Number q) {
        if (q.type != Type.QUATERNION)
            throw new IllegalArgumentException("quaternionConjugate expects QUATERNION, got " + q.type);
        Number ar = (q.real  != null) ? q.real  : ZERO_INT;
        Number ai = (q.ipart != null) ? q.ipart : ZERO_INT;
        Number aj = (q.jpart != null) ? q.jpart : ZERO_INT;
        Number ak = (q.kpart != null) ? q.kpart : ZERO_INT;
        return Number.quaternion(ar, negate(ai), negate(aj), negate(ak));
    }

    public static Number quaternionInverse(Number q) {
        if (q.type != Type.QUATERNION)
            throw new IllegalArgumentException("quaternionInverse expects QUATERNION, got " + q.type);
        return reciprocalQuaternion(q);
    }


    private static boolean isExactScalar(Number n) {
        return switch (n.type) {
            case INT, BIGINT, RATIONAL, BIGRATIONAL -> true;
            default -> false;
        };
    }

    private static boolean isExactNumber(Number n) {
        return switch (n.type) {
            case INT, BIGINT, RATIONAL, BIGRATIONAL -> true;
            case FLOAT, BIGFLOAT -> false;
            case COMPLEX -> isExactNumber(n.real != null ? n.real : ZERO_INT)
                          && isExactNumber(n.ipart != null ? n.ipart : ZERO_INT);
            case QUATERNION -> isExactNumber(n.real != null ? n.real : ZERO_INT)
                              && isExactNumber(n.ipart != null ? n.ipart : ZERO_INT)
                              && isExactNumber(n.jpart != null ? n.jpart : ZERO_INT)
                              && isExactNumber(n.kpart != null ? n.kpart : ZERO_INT);
        };
    }

    private static Number squaredMagnitudeExact(Number n) {
        switch (n.type) {
            case INT:
            case BIGINT:
            case RATIONAL:
            case BIGRATIONAL:
                return multiply(n, n);
            case FLOAT:
            case BIGFLOAT:
                return null;
            case COMPLEX: {
                Number realPart = (n.real != null) ? n.real : ZERO_INT;
                Number imagPart = (n.ipart != null) ? n.ipart : ZERO_INT;
                if (!isExactNumber(realPart) || !isExactNumber(imagPart))
                    return null;
                Number realSq = multiply(realPart, realPart);
                Number imagSq = multiply(imagPart, imagPart);
                return add(realSq, imagSq);
            }
            case QUATERNION: {
                Number ar = (n.real != null) ? n.real : ZERO_INT;
                Number ai = (n.ipart != null) ? n.ipart : ZERO_INT;
                Number aj = (n.jpart != null) ? n.jpart : ZERO_INT;
                Number ak = (n.kpart != null) ? n.kpart : ZERO_INT;
                if (!isExactNumber(ar) || !isExactNumber(ai) || !isExactNumber(aj) || !isExactNumber(ak))
                    return null;
                Number sum = add(multiply(ar, ar), multiply(ai, ai));
                sum = add(sum, multiply(aj, aj));
                sum = add(sum, multiply(ak, ak));
                return sum;
            }
            default:
                throw new IllegalStateException("Unknown numeric type: " + n.type);
        }
    }

    private static double magnitudeDouble(Number n) {
        switch (n.type) {
            case INT:
            case BIGINT:
            case RATIONAL:
            case BIGRATIONAL:
            case FLOAT:
            case BIGFLOAT:
                return Math.abs(toDouble(n));
            case COMPLEX: {
                double realPart = toDouble(n.real != null ? n.real : ZERO_INT);
                double imagPart = toDouble(n.ipart != null ? n.ipart : ZERO_INT);
                return Math.hypot(realPart, imagPart);
            }
            case QUATERNION: {
                double realPart = toDouble(n.real != null ? n.real : ZERO_INT);
                double iPart    = toDouble(n.ipart != null ? n.ipart : ZERO_INT);
                double jPart    = toDouble(n.jpart != null ? n.jpart : ZERO_INT);
                double kPart    = toDouble(n.kpart != null ? n.kpart : ZERO_INT);
                double sumSq = realPart * realPart + iPart * iPart + jPart * jPart + kPart * kPart;
                return Math.sqrt(sumSq);
            }
            default:
                throw new IllegalStateException("Unknown numeric type: " + n.type);
        }
    }

    private static int compareScalar(Number a, Number b) {
        if (isExactScalar(a) && isExactScalar(b)) {
            BigInteger[] fa = toBigFraction(a);
            BigInteger[] fb = toBigFraction(b);
            BigInteger left = fa[0].multiply(fb[1]);
            BigInteger right = fb[0].multiply(fa[1]);
            return left.compareTo(right);
        }
        BigDecimal left = toBigDecimal(a);
        BigDecimal right = toBigDecimal(b);
        return left.compareTo(right);
    }

    private static int compareNumbers(Number a, Number b) {
        if (isScalar(a) && isScalar(b)) {
            return compareScalar(a, b);
        }

        Number exactA = squaredMagnitudeExact(a);
        Number exactB = squaredMagnitudeExact(b);
        if (exactA != null && exactB != null) {
            return compareScalar(exactA, exactB);
        }

        double magA = magnitudeDouble(a);
        double magB = magnitudeDouble(b);
        return Double.compare(magA, magB);
    }

    public static Number magnitude(Number n) {
        Number exactSq = squaredMagnitudeExact(n);
        if (exactSq != null) {
            double approx = Math.sqrt(Math.abs(toDouble(exactSq)));
            return Number.real(approx);
        }
        return Number.real(magnitudeDouble(n));
    }

    public static boolean numericEquals(Number a, Number b) {
        return compareNumbers(a, b) == 0;
    }

    public static boolean lessThan(Number a, Number b) {
        return compareNumbers(a, b) < 0;
    }

    public static boolean greaterThan(Number a, Number b) {
        return compareNumbers(a, b) > 0;
    }


    //------ Exact → Inexact conversions -------
    public static Number toInexact(Number n) {
        return switch (n.type) {
            case FLOAT -> n;
            case BIGFLOAT -> Number.real(n.bigFloatVal.doubleValue());
            case INT, RATIONAL -> Number.real(toDouble(n));
            case BIGINT, BIGRATIONAL -> Number.real(toDouble(n));
            case COMPLEX -> {
                Number realPart = toInexact(n.real != null ? n.real : ZERO_INT);
                Number imagPart = toInexact(n.ipart != null ? n.ipart : ZERO_INT);
                yield Number.complex(realPart, imagPart);
            }
            case QUATERNION -> {
                Number realPart = toInexact(n.real != null ? n.real : ZERO_INT);
                Number iPart = toInexact(n.ipart != null ? n.ipart : ZERO_INT);
                Number jPart = toInexact(n.jpart != null ? n.jpart : ZERO_INT);
                Number kPart = toInexact(n.kpart != null ? n.kpart : ZERO_INT);
                yield Number.quaternion(realPart, iPart, jPart, kPart);
            }
        };
    }

    public static Number toInexactBig(Number n) {
        return switch (n.type) {
            case BIGFLOAT -> n;
            case FLOAT -> Number.real(BigDecimal.valueOf(n.floatVal));
            case INT -> Number.real(BigDecimal.valueOf(n.intVal));
            case BIGINT -> Number.real(new BigDecimal(n.bigVal));
            case RATIONAL -> {
                BigDecimal value = BigDecimal.valueOf(n.num)
                        .divide(BigDecimal.valueOf(n.den), MathContext.DECIMAL128);
                yield Number.real(value);
            }
            case BIGRATIONAL -> {
                BigDecimal value = new BigDecimal(n.bigNum)
                        .divide(new BigDecimal(n.bigDen), MathContext.DECIMAL128);
                yield Number.real(value);
            }
            case COMPLEX -> {
                Number realPart = toInexactBig(n.real != null ? n.real : ZERO_INT);
                Number imagPart = toInexactBig(n.ipart != null ? n.ipart : ZERO_INT);
                yield Number.complex(realPart, imagPart);
            }
            case QUATERNION -> {
                Number realPart = toInexactBig(n.real != null ? n.real : ZERO_INT);
                Number iPart = toInexactBig(n.ipart != null ? n.ipart : ZERO_INT);
                Number jPart = toInexactBig(n.jpart != null ? n.jpart : ZERO_INT);
                Number kPart = toInexactBig(n.kpart != null ? n.kpart : ZERO_INT);
                yield Number.quaternion(realPart, iPart, jPart, kPart);
            }
        };
    }


   

    @Override
    public String toString() {
        return switch (type) {
            case INT          -> Long.toString(intVal);
            case BIGINT       -> bigVal.toString();
            case FLOAT        -> Double.toString(floatVal);
            case BIGFLOAT     -> bigFloatVal.toPlainString();
            case RATIONAL     -> (den == 1) ? Long.toString(num) : num + "/" + den;
            case BIGRATIONAL  -> (bigDen.equals(BigInteger.ONE))
                                    ? bigNum.toString()
                                    : "(" + bigNum + "/" + bigDen + ")";
            case COMPLEX      -> formatComplex(this);
            case QUATERNION   -> formatQuaternion(this);
        };
    }




    public static void main(String[] args) {
        System.out.println("=== BASIC TYPES ===");
        Number i  = Number.integer(100);
        Number k  = Number.integer(new BigInteger("6000000000000000000000"));
        Number f  = Number.real(5300.12347);
        Number bf = Number.real(new BigDecimal("123456789.987654321"));
        Number r  = Number.rational(300, 200); // 3/2
        Number br = Number.rational(new BigInteger("12345678901234567890"),
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

        System.out.println("=== RATIONAL FACTORY (MIXED TYPES) ===");
        Number rn1 = Number.rational(Number.integer(2), Number.integer(4));
        Number rn2 = Number.rational(Number.integer(2), Number.integer(BigInteger.valueOf(4)));
        Number rn3 = Number.rational(Number.integer(BigInteger.valueOf(6)), Number.integer(BigInteger.valueOf(9)));
        System.out.println("rational(int,int)    = " + rn1 + "   (expected 1/2)");
        System.out.println("rational(int,bigint) = " + rn2 + "   (expected 1/2)");
        System.out.println("rational(big,big)    = " + rn3 + "   (expected 2/3)");
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
        System.out.println("bigfloat + bigfloat  = " + Number.add(Number.real(veryBig), Number.real(veryBig)));
        System.out.println();

        System.out.println("=== EDGE CASES ===");
        System.out.println("zero + zero          = " + Number.add(Number.integer(0), Number.integer(0)));
        System.out.println("negatives            = " + Number.add(Number.integer(-5), Number.integer(2)) + "   (expected -3)");
        System.out.println("rational + negative  = " + Number.add(Number.rational(1, 3), Number.integer(-1)) + "   (expected -2/3)");


        System.out.println("=== MULTIPLICATION TESTS ===");
        System.out.println("int * int            = " + Number.multiply(Number.integer(5), Number.integer(7)) + "   (expected 35)");
        System.out.println("bigint * int         = " + Number.multiply(k, i) + "   (expected 600000000000000000000000)");
        System.out.println("float * float        = " + Number.multiply(Number.real(1.25), Number.real(3.75)) + "   (expected 4.6875)");
        System.out.println("rational * rational  = " + Number.multiply(Number.rational(2, 3), Number.rational(3, 4)) + "   (expected 1/2)");
        System.out.println("rational * int       = " + Number.multiply(Number.rational(3, 2), Number.integer(2)) + "   (expected 3)");
        System.out.println("int * rational       = " + Number.multiply(Number.integer(2), Number.rational(3, 2)) + "   (expected 3)");
        System.out.println("rational * bigint    = " + Number.multiply(Number.rational(3, 2), k) + "   (expected 9000000000000000000000)");
        System.out.println();

        System.out.println("=== BIG PROMOTIONS ===");
        // overflow test for int * int -> bigint
        Number bigMul1 = Number.integer(Long.MAX_VALUE);
        Number bigMul2 = Number.integer(2);
        System.out.println("overflow test (long * 2) = " + Number.multiply(bigMul1, bigMul2) + "   (expected 18446744073709551614 as BigInteger)");

        // rational overflow to bigRational
        Number largeRM1 = Number.rational(Long.MAX_VALUE / 2, 3);
        Number largeRM2 = Number.rational(2, 3);
        System.out.println("rational overflow => bigRational = " + Number.multiply(largeRM1, largeRM2));
        System.out.println();

        System.out.println("=== MIXED MULTIPLICATION ===");
        System.out.println("float * int          = " + Number.multiply(Number.real(2.5), Number.integer(3)) + "   (expected 7.5)");
        System.out.println("float * rational     = " + Number.multiply(Number.real(0.5), Number.rational(3, 2)) + "   (expected 0.75)");
        System.out.println("bigfloat * float     = " + Number.multiply(bf, f) + "   (expected ~654012121365.2799)");
        System.out.println("rational * bigfloat  = " + Number.multiply(r, bf) + "   (expected ~185185184.9814814815)");
        System.out.println();

        System.out.println("=== COMPLEX MULTIPLICATION ===");
        Number c3 = Number.complex(Number.integer(1), Number.integer(2));  // 1 + 2i
        Number c4 = Number.complex(Number.integer(3), Number.integer(4));  // 3 + 4i
        System.out.println("complex * complex    = " + Number.multiply(c3, c4) + "   (expected (-5 + 10i))");
        System.out.println("real * complex       = " + Number.multiply(Number.integer(2), c3) + "   (expected (2 + 4i))");
        System.out.println("rational * complex   = " + Number.multiply(Number.rational(1, 2), c3) + "   (expected (1/2 + i))");
        System.out.println();

        System.out.println("=== BIGFLOAT PRECISION (MULT) ===");
        BigDecimal bigPrec = new BigDecimal("1.0000000000000000000000000000000001");
        System.out.println("bigfloat * bigfloat  = " + Number.multiply(Number.real(bigPrec), Number.real(bigPrec))
                        + "   (expected ~1.0000000000000000000000000000000002)");
        System.out.println();

        System.out.println("=== EDGE CASES (MULT) ===");
        System.out.println("zero * any           = " + Number.multiply(Number.zero(Type.INT), Number.integer(999)) + "   (expected 0)");
        System.out.println("negatives            = " + Number.multiply(Number.integer(-5), Number.integer(2)) + "   (expected -10)");
        System.out.println("rational * negative  = " + Number.multiply(Number.rational(1, 3), Number.integer(-3)) + "   (expected -1)");
        System.out.println("complex * zero       = " + Number.multiply(c3, Number.zero(Type.COMPLEX)) + "   (expected 0 + 0i)");
        System.out.println();
    
        System.out.println("=== DIVISION TESTS ===");

        // --- Simple divisions ---
        System.out.println("int / int            = " + Number.divide(Number.integer(7), Number.integer(2)) + "   (expected 7/2)");
        System.out.println("int / int exact      = " + Number.divide(Number.integer(8), Number.integer(2)) + "   (expected 4)");
        System.out.println("bigint / int         = " + Number.divide(k, Number.integer(2)) + "   (expected 3000000000000000000000)");
        System.out.println("rational / rational  = " + Number.divide(Number.rational(3, 4), Number.rational(2, 3)) + "   (expected 9/8)");
        System.out.println("rational / int       = " + Number.divide(Number.rational(3, 2), Number.integer(3)) + "   (expected 1/2)");
        System.out.println("int / rational       = " + Number.divide(Number.integer(3), Number.rational(3, 2)) + "   (expected 2)");
        System.out.println();

        // --- Big promotions ---
        Number bigDiv1 = Number.integer(Long.MAX_VALUE);
        Number bigDiv2 = Number.integer(2);
        System.out.println("overflow test (long / 2) = " + Number.divide(bigDiv1, bigDiv2) + "   (expected 4611686018427387903)");
        System.out.println("bigint / bigint exact     = " + Number.divide(k, Number.integer(3)) + "   (expected 2000000000000000000000)");
        System.out.println("bigint / bigint rational  = " + Number.divide(k, Number.integer(7)) + "   (expected bigRational)");
        System.out.println();

        // --- Mixed division ---
        System.out.println("float / int          = " + Number.divide(Number.real(7.5), Number.integer(3)) + "   (expected 2.5)");
        System.out.println("float / rational     = " + Number.divide(Number.real(1.5), Number.rational(3, 2)) + "   (expected 1)");
        System.out.println("bigfloat / float     = " + Number.divide(bf, f) + "   (expected ~23300.0000189)");
        System.out.println("rational / bigfloat  = " + Number.divide(r, bf) + "   (expected ~0.000000012145)");
        System.out.println();

        // --- Complex division ---
        Number c5 = Number.complex(Number.integer(1), Number.integer(2));  // 1 + 2i
        Number c6 = Number.complex(Number.integer(3), Number.integer(4));  // 3 + 4i
        System.out.println("complex / complex    = " + Number.divide(c5, c6) + "   (expected (11/25 + 2/25i))");
        System.out.println("complex / real       = " + Number.divide(c5, Number.integer(2)) + "   (expected (1/2 + i))");
        System.out.println("real / complex       = " + Number.divide(Number.integer(1), c6) + "   (expected (3/25 - 4/25i))");
        System.out.println();

        // --- Edge cases ---
        System.out.println("zero / nonzero       = " + Number.divide(Number.integer(0), Number.integer(7)) + "   (expected 0)");
        try {
            System.out.println("nonzero / zero       = " + Number.divide(Number.integer(7), Number.integer(0)));
        } catch (ArithmeticException e) {
            System.out.println("nonzero / zero       = Exception (expected)");
        }
        try {
            System.out.println("complex / 0+0i       = " + Number.divide(c5, Number.complex(Number.integer(0), Number.integer(0))));
        } catch (ArithmeticException e) {
            System.out.println("complex / 0+0i       = Exception (expected)");
        }
        System.out.println();

        // =======================
        // === QUATERNION TESTS ===
        // =======================
        System.out.println("=== QUATERNION BASICS ===");
        Number Qi = Number.quaternion(Number.integer(0), Number.integer(1), Number.integer(0), Number.integer(0)); // i
        Number Qj = Number.quaternion(Number.integer(0), Number.integer(0), Number.integer(1), Number.integer(0)); // j
        Number Qk = Number.quaternion(Number.integer(0), Number.integer(0), Number.integer(0), Number.integer(1)); // k
        Number Q1 = Number.quaternion(Number.integer(1), Number.integer(0), Number.integer(0), Number.integer(0)); // 1

        Number qA = Number.quaternion(Number.integer(1), Number.integer(2), Number.integer(3), Number.integer(4)); // 1+2i+3j+4k
        Number qB = Number.quaternion(Number.integer(5), Number.integer(6), Number.integer(7), Number.integer(8)); // 5+6i+7j+8k
        System.out.println("qA = " + qA + "   (expected 1 + 2i + 3j + 4k)");
        System.out.println("qB = " + qB + "   (expected 5 + 6i + 7j + 8k)");
        System.out.println("qA + qB           = " + Number.add(qA, qB) + "   (expected 6 + 8i + 10j + 12k)");
        System.out.println("qB - qA           = " + Number.add(qB, Number.negate(qA)) + "   (expected 4 + 4i + 4j + 4k)");
        // If negate is private, you can simulate subtraction by: qB + (-1)*qA
        // Number.add(qB, Number.multiply(Number.integer(-1), qA))

        System.out.println();
        System.out.println("=== HAMILTON RULES ===");
        System.out.println("i * j = " + Number.multiply(Qi, Qj) + "   (expected 0 + 0i + 0j + 1k)");
        System.out.println("j * k = " + Number.multiply(Qj, Qk) + "   (expected 0 + 1i + 0j + 0k)");
        System.out.println("k * i = " + Number.multiply(Qk, Qi) + "   (expected 0 + 0i + 1j + 0k)");
        System.out.println("j * i = " + Number.multiply(Qj, Qi) + "   (expected 0 + 0i + 0j + -1k)");
        System.out.println("k * j = " + Number.multiply(Qk, Qj) + "   (expected 0 + -1i + 0j + 0k)");
        System.out.println("i * k = " + Number.multiply(Qi, Qk) + "   (expected 0 + 0i + -1j + 0k)");
        System.out.println("i * i = " + Number.multiply(Qi, Qi) + "   (expected -1 + 0i + 0j + 0k)");
        System.out.println("j * j = " + Number.multiply(Qj, Qj) + "   (expected -1 + 0i + 0j + 0k)");
        System.out.println("k * k = " + Number.multiply(Qk, Qk) + "   (expected -1 + 0i + 0j + 0k)");

        System.out.println();
        System.out.println("=== QUATERNION * SCALAR (INT/RATIONAL/FLOAT/BIGFLOAT) ===");
        System.out.println("2 * (1+i+j+k)       = " + Number.multiply(Number.integer(2), Number.quaternion(Number.integer(1), Number.integer(1), Number.integer(1), Number.integer(1))) + "   (expected 2 + 2i + 2j + 2k)");
        System.out.println("(3+6i+0j+0k) / 1.5  = " + Number.divide(Number.quaternion(Number.integer(3), Number.integer(6), Number.integer(0), Number.integer(0)), Number.real(1.5)) + "   (expected 2 + 4i + 0j + 0k)");
        System.out.println("(2/3) * (3+6i+9j+12k) = " + Number.multiply(Number.rational(2,3), Number.quaternion(Number.integer(3), Number.integer(6), Number.integer(9), Number.integer(12))) + "   (expected 2 + 4i + 6j + 8k)");

        System.out.println("bigfloat * (1+i)    = " + Number.multiply(Number.real(new BigDecimal("1.0000000000000000000000000000000001")),Number.quaternion(Number.integer(1), Number.integer(1), Number.integer(0), Number.integer(0))) + "   (expected ~1.000... + 1.000...i)");
        System.out.println();
        System.out.println("=== COMPLEX ⟷ QUATERNION PROMOTION ===");
        Number cA = Number.complex(Number.integer(1), Number.integer(2)); // 1+2i
        System.out.println("(1+2i) * (3+4i+5j+6k) = " + Number.multiply(cA, Number.quaternion(Number.integer(3), Number.integer(4), Number.integer(5), Number.integer(6))) + "   (expected quaternion result)");
        System.out.println("(1+i) * j           = " + Number.multiply(Number.complex(Number.integer(1), Number.integer(1)), Qj) + "   (expected 0 + 0i + 1j + 1k)");
        System.out.println("j * (1+i)           = " + Number.multiply(Qj, Number.complex(Number.integer(1), Number.integer(1))) + "   (expected 0 + 0i + 1j - 1k)  // anti-commutativity shows up");

        // (For subtraction without public negate, simulate as above — multiply by -1 and add)

        System.out.println();
        System.out.println("=== RECIPROCAL & DIVISION ===");
        System.out.println("qA / qA             = " + Number.divide(qA, qA) + "   (expected 1 + 0i + 0j + 0k)");
        System.out.println("(6+8i+10j+12k)/2    = " + Number.divide(Number.quaternion(Number.integer(6), Number.integer(8), Number.integer(10), Number.integer(12)), Number.integer(2)) + "   (expected 3 + 4i + 5j + 6k)");
        try {
            System.out.println("div by zero quat    = " + Number.divide(qA, Number.quaternion(Number.integer(0), Number.integer(0), Number.integer(0), Number.integer(0))));
        } catch (ArithmeticException e) {
            System.out.println("div by zero quat    = Exception (expected)");
        }

        System.out.println();
        System.out.println("=== DISTRIBUTIVITY (SPOT CHECK) ===");
        Number qX = Number.quaternion(Number.integer(2), Number.integer(1), Number.integer(0), Number.integer(1)); // 2 + i + k
        Number qY = Number.quaternion(Number.integer(1), Number.integer(1), Number.integer(1), Number.integer(0)); // 1 + i + j
        Number qZ = Number.quaternion(Number.integer(0), Number.integer(2), Number.integer(1), Number.integer(1)); // 2i + j + k
        Number left = Number.multiply(qX, Number.add(qY, qZ));
        Number right = Number.add(Number.multiply(qX, qY), Number.multiply(qX, qZ));
        System.out.println("qX*(qY+qZ)          = " + left);
        System.out.println("qX*qY + qX*qZ       = " + right);
        System.out.println("distributive equal?  (manual check: the two lines above should match)");

        System.out.println();
        System.out.println("=== ZERO/ONE IDENTITIES ===");
        System.out.println("qA + 0              = " + Number.add(qA, Number.zero(Number.Type.QUATERNION)) + "   (expected qA)");
        System.out.println("qA * 1              = " + Number.multiply(qA, Number.one(Number.Type.QUATERNION)) + "   (expected qA)");

        System.out.println();
        System.out.println("=== POWER TESTS ===");
        System.out.println("2^10               = " + Number.pow(Number.integer(2), Number.integer(10)) + "   (expected 1024)");
        System.out.println("(3/2)^3             = " + Number.pow(Number.rational(3, 2), Number.integer(3)) + "   (expected 27/8)");
        System.out.println("(2.0)^(0.5)         = " + Number.pow(Number.real(2.0), Number.real(0.5)) + "   (expected ~1.41421356237)");
        System.out.println("(1+2i)^3            = " + Number.pow(Number.complex(Number.integer(1), Number.integer(2)), Number.integer(3)) + "   (expected -11-2i)");
        System.out.println("Quaternion^2        = " + Number.pow(qA, Number.integer(2)) + "   (expected quaternion result)");

        System.out.println();
        System.out.println("=== MOD TESTS ===");
        System.out.println("17 mod 5            = " + Number.mod(Number.integer(17), Number.integer(5)) + "   (expected 2)");
        System.out.println("(-7) mod 3          = " + Number.mod(Number.integer(-7), Number.integer(3)) + "   (expected 2)");
        System.out.println("(9/4) mod (2/3)     = " + Number.mod(Number.rational(9, 4), Number.rational(2, 3)) + "   (expected 1/4)");
        System.out.println("5.5 mod 2.0         = " + Number.mod(Number.real(5.5), Number.real(2.0)) + "   (expected 1.5)");
        System.out.println("Complex mod 2       = " + Number.mod(Number.complex(Number.integer(5), Number.integer(3)), Number.integer(2)) + "   (expected (1+1i))");
        System.out.println("Quaternion mod 3    = " + Number.mod(qA, Number.integer(3)) + "   (expected component-wise result)");

        System.out.println();
        System.out.println("=== INEXACT CONVERSIONS ===");
        Number exactR = Number.rational(7, 3);
        Number exactBR = Number.rational(new BigInteger("12345678901234567890"), new BigInteger("12345"));
        Number exactComplex = Number.complex(Number.rational(5, 4), Number.integer(2));
        System.out.println("toInexact 7/3       = " + Number.toInexact(exactR) + "   (expected ~2.3333333333)");
        System.out.println("toInexact bigRat    = " + Number.toInexactBig(exactBR) + "   (expected bigfloat ~1000000065.0)");
        System.out.println("toInexact complex   = " + Number.toInexact(exactComplex) + "   (expected (~1.25 + 2i))");
        System.out.println("toInexactBig qA     = " + Number.toInexactBig(qA) + "   (expected quaternion with bigfloat components)");

        System.out.println();
        System.out.println("=== CONJUGATE & INVERSE ===");
        System.out.println("conj(1+2i)          = " + Number.complexConjugate(c3) + "   (expected 1+-2i)");
        System.out.println("conj(qA)            = " + Number.quaternionConjugate(qA) + "   (expected 1+-2i+-3j+-4k)");
        System.out.println("inverse(qA)         = " + Number.quaternionInverse(qA) + "   (expected quaternion inverse)");

        System.out.println();
        System.out.println("=== MAGNITUDE & COMPARISON ===");
        System.out.println("|1+2i|              = " + Number.magnitude(c3) + "   (expected ~2.2360679)");
        System.out.println("|qA|                = " + Number.magnitude(qA) + "   (expected ~5.4772256)");
        System.out.println("numericEquals 2,4/2 = " + Number.numericEquals(Number.integer(2), Number.rational(4, 2)) + "   (expected true)");
        System.out.println("lessThan 3/2, 2     = " + Number.lessThan(Number.rational(3, 2), Number.integer(2)) + "   (expected true)");
        System.out.println("greaterThan qA,qB   = " + Number.greaterThan(qA, qB) + "   (expected false)");
        System.out.println("lessThan (1+2i),3   = " + Number.lessThan(c3, Number.integer(3)) + "   (expected true)");
    }

}
