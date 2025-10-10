import java.util.BigInteger;

public final class Number {
    enum Type { INT, BIGINT, FLOAT, RATIONAL, COMPLEX }

    private final Type type;
    private final long intVal;
    private final BigInteger bigVal;
    private final double floatVal;
    private final long num;  // rational numerator
    private final long den;  // rational denominator
    private final Number real; // complex real part
    private final Number imag; // complex imaginary part
    
    public static Number of(long value){
        Type = INT;
        intVal = value;
    }
    
    public static Number of(BigInteger value){
        Type BIGINT;
        bigVal = value;
    }

    public static Number rational(long p, long q){
        type = RATIONAL;
        long g = gcd(p,q);
        num = p/g;
        den = q/g;
    }

    public static Number complex(num  rel, num img){
        type = COMPLEX;
        real = rel;
        imag = img;
    }

    @Override
    public string toString(){
      switch(type){
        case INT:
            return intVal.toString();
        case BIGINT:
            return bigVal.toString();
        case RATIONAL:
            return num +"|"+ den;
        case COMPLEX:
            return real+"+"+imag+"i";
      }
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }
}
