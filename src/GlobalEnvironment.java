import java.util.function.Function;
import java.util.function.BiFunction;
import java.math.BigInteger;

public class GlobalEnvironment {
    public static Environment initGlobalEnvironment(){
        Environment environment = new Environment(
            new Pair<>("else", "#t"),
            new Pair<>("null?", (Function<Object,String>) (x) -> {
                if (x == null) return "#t";
                if (x instanceof LinkedList<?> list) {
                    if (list.isEmpty()) return "#t";
                    if (list.isCharList() && list.size() == 0) return "#t";
                    return "#f";
                }
                if (x instanceof String s) {
                    return s.isEmpty() ? "#t" : "#f";
                }
                return "#f";
            }),                                
            new Pair<>("number?", (Function<Object,String>) (x) -> {
                if (x instanceof Number) return "#t";
                return "#f";
            }),
            new Pair<>("symbol?", (Function<Object,String>) (x) -> {
                if (x instanceof Symbol) return "#t";
                return "#f";
            }),
            new Pair<>("list?", (Function<Object,String>) (x) -> {
                if (x instanceof LinkedList) return "#t";
                return "#f";
            }),
            new Pair<>("even?", (Function<Object, String>) (x) -> {
                if (!(x instanceof Number n))
                    throw new RuntimeException("even?: expected number, got " + x);
                Number remainder = Number.mod(n, Number.integer(2));
                return Number.numericEquals(remainder, Number.zero(remainder)) ? "#t" : "#f";
            }),
            new Pair<>("odd?", (Function<Object, String>) (x) -> {
                if (!(x instanceof Number n))
                    throw new RuntimeException("odd?: expected number, got " + x);
                Number remainder = Number.mod(n, Number.integer(2));
                return Number.numericEquals(remainder, Number.zero(remainder)) ? "#f" : "#t";
            }),
            new Pair<>("!",(Function<String, String>) (x) -> x.equals("#t") ? "#f" : "#t"),
            new Pair<>("and", (BiFunction<String, String, String>) (p, q) ->
                (p.equals("#t") && q.equals("#t")) ? "#t" : "#f"
            ),
            new Pair<>("not", (Function<String,String>) (x) -> x.equals("#t") ? "#f" : "#t"),
            new Pair<>("or", (BiFunction<String, String, String>) (p, q) ->
                (p.equals("#t") || q.equals("#t")) ? "#t" : "#f"
            ),
            new Pair<>("xor", (BiFunction<String, String, String>) (p, q) ->
                ((p.equals("#t") && !q.equals("#t")) || (q.equals("#t") && !p.equals("#t"))) ? "#t" : "#f"
            ),
            new Pair<>("head", (Function<Object,Object>) (x) -> {
                if (x instanceof LinkedList<?> list) {
                    Object head = list.head();
                    if (list.isCharList()) {
                        if (head == null) {
                            return LinkedList.fromString("");
                        }
                        if (head instanceof String s) {
                            return LinkedList.fromString(s);
                        }
                    }
                    return head;
                }
                throw new RuntimeException("head: unsupported type " + x.getClass());
            }),
            new Pair<>("tail", (Function<Object,Object>) (x) -> {
                if (x instanceof LinkedList<?> list) {
                    Object tail = list.tail();
                    if (tail == null) {
                        if (list.isCharList()) {
                            return LinkedList.fromString("");
                        }
                        return new LinkedList<>();
                    }
                    return tail;
                }
                throw new RuntimeException("tail: unsupported type " + x.getClass());
            }),                
            new Pair<>("length", (Function<LinkedList, Number>) (xs) ->
                Number.integer(xs.size())
            ),
            new Pair<>("print", (Function<Object, Object>) x1 -> {
                if (x1 == null) {
                    System.out.println("()");
                    return LinkedList.fromString("");
                }
                String out;
                if (x1 instanceof LinkedList<?> list && list.isCharList()) {
                    out = LinkedList.listToRawString(list);
                } else {
                    out = x1.toString();
                    if (out.length() >= 2 && out.startsWith("\"") && out.endsWith("\"")) {
                        out = out.substring(1, out.length() - 1);
                    }
                }
                System.out.println(out);
                return LinkedList.fromString("");
            }),
            new Pair<>("printf", (Function<Object, Object>) x1 -> {
                if (x1 == null) return LinkedList.fromString("");
                String out;
                if (x1 instanceof LinkedList<?> list && list.isCharList()) {
                    out = LinkedList.listToRawString(list);
                } else {
                    out = x1.toString();
                    if (out.length() >= 2 && out.startsWith("\"") && out.endsWith("\"")) {
                        out = out.substring(1, out.length() - 1);
                    }
                }
                System.out.print(unescapeJava(out));
                return LinkedList.fromString("");
            }),
            new Pair<>("to-inexact", (Function<Number, Number>) n -> Number.toInexact(n)),
            new Pair<>("to-inexact-big", (Function<Number, Number>) n -> Number.toInexactBig(n))
        );
        environment.addFrame(
            new Pair<>("eval", (Function<Object,Object>) (str) -> {
            Parser p = new Parser(str.toString());
            return Evaluator.eval(p.parse(), environment);
            }),
            new Pair<>("cons", (BiFunction<Object,Object,LinkedList>) (fst, snd) -> {
                if (fst == null) {
                    throw new SyntaxException("First element of a pair cannot be null");
                }
                if (LinkedList.isCharList(snd)) {
                    LinkedList<?> tailList = (LinkedList<?>) snd;
                    if (LinkedList.isCharList(fst)) {
                        return LinkedList.concatCharLists((LinkedList<?>) fst, tailList);
                    }
                    if (fst instanceof String s && s.length() > 0) {
                        LinkedList<String> headList = LinkedList.fromString(s);
                        return LinkedList.concatCharLists(headList, tailList);
                    }
                }
                if (snd == null) {
                    return new LinkedList<>(fst, (LinkedList<Object>) null);
                }
                if (snd instanceof LinkedList<?> tailList) {
                    @SuppressWarnings("unchecked")
                    LinkedList<Object> properTail = (LinkedList<Object>) tailList;
                    return new LinkedList<>(fst, properTail);
                }
                return new LinkedList<>(fst, snd);
            }) 
        );
        environment.addFrame(
            new Pair<>("+", (Function<LinkedList<?>, Object>) (args) -> {
                if (args == null || args.head() == null) {
                    return Number.integer(0);
                }

                Object first = args.head();
                if (first instanceof Vector vec) {
                    LinkedList<?> current = (args.tail() instanceof LinkedList<?> next) ? next : null;
                    Vector acc = vec;
                    while (current != null && current.head() != null) {
                        Object element = current.head();
                        if (!(element instanceof Vector other)) {
                            throw new RuntimeException("+: mixed vector/non-vector arguments");
                        }
                        acc = Number.add(acc, other);
                        Object tail = current.tail();
                        current = (tail instanceof LinkedList<?> nextTail) ? nextTail : null;
                    }
                    return acc;
                }

                Number sum = Number.integer(0);
                LinkedList<?> current = args;
                while (current != null && current.head() != null) {
                    Object head = current.head();
                    if (!(head instanceof Number n))
                        throw new RuntimeException("+: expected number, got " + head + " (type " + head.getClass() + ")");
                    sum = Number.add(sum, n);
                    Object tail = current.tail();
                    if (!(tail instanceof LinkedList<?> next)) break;
                    current = next;
                }
                return sum;
            }),
            new Pair<>("-", (Function<LinkedList<?>, Object>) (args) -> {
                if (args == null || args.head() == null)
                    return Number.integer(0);
                Object first = args.head();
                Object tail = args.tail();

                if (first instanceof Vector vec) {
                    if (tail == null) {
                        return Number.multiply(Number.integer(-1), vec);
                    }
                    if (!(tail instanceof LinkedList<?> current)) {
                        return vec;
                    }
                    Vector acc = vec;
                    while (current.head() != null) {
                        Object head = current.head();
                        if (!(head instanceof Vector other))
                            throw new RuntimeException("-: mixed vector/non-vector arguments");
                        acc = Number.sub(acc, other);
                        Object nextTail = current.tail();
                        if (!(nextTail instanceof LinkedList<?> next)) break;
                        current = next;
                    }
                    return acc;
                }

                if (!(first instanceof Number result))
                    throw new RuntimeException("-: expected number, got " + first);
                if (tail == null) {
                    return Number.sub(Number.zero(result), result);
                }
                if (!(tail instanceof LinkedList<?> current)) return result;
                while (current.head() != null) {
                    Object head = current.head();
                    if (!(head instanceof Number n))
                        throw new RuntimeException("-: expected number, got " + head + " (type " + head.getClass() + ")");
                    result = Number.sub(result, n);
                    Object nextTail = current.tail();
                    if (!(nextTail instanceof LinkedList<?> next)) break;
                    current = next;
                }
                return result;
            }),
            new Pair<>("*", (Function<LinkedList<?>, Object>) (args) -> {
                if (args == null || args.head() == null) {
                    return Number.integer(1);
                }
                Object first = args.head();
                LinkedList<?> current = args.tail() instanceof LinkedList<?> next ? next : null;

                if (first instanceof Vector vec) {
                    if (current == null || current.head() == null)
                        throw new RuntimeException("*: vector multiplication requires scalar argument");
                    Object scalarObj = current.head();
                    if (!(scalarObj instanceof Number scalar))
                        throw new RuntimeException("*: expected scalar with vector, got " + scalarObj);
                    if (current.tail() != null && current.tail() instanceof LinkedList<?> more && more.head() != null)
                        throw new RuntimeException("*: too many arguments after vector");
                    return Number.multiply(vec, scalar);
                }

                Number prod = Number.integer(1);
                current = args;
                while (current != null && current.head() != null) {
                    Object head = current.head();
                    if (!(head instanceof Number n))
                        throw new RuntimeException("*: expected number, got " + head + " (type " + head.getClass() + ")");
                    prod = Number.multiply(prod, n);
                    Object tail = current.tail();
                    if (!(tail instanceof LinkedList<?> next)) break;
                    current = next;
                }
                return prod;
            }),
            new Pair<>("/", (Function<LinkedList<?>, Object>) (args) -> {
                if (args == null || args.head() == null)
                    throw new SyntaxException("/ expects at least one argument");
                Object first = args.head();
                if (!(first instanceof Number result))
                    throw new RuntimeException("/: expected number, got " + first + " (type " + first.getClass() + ")");
                Object tail = args.tail();
                if (!(tail instanceof LinkedList<?> current))
                    return result;
                while (current.head() != null) {
                    Object head = current.head();
                    if (!(head instanceof Number n))
                        throw new RuntimeException("/: expected number, got " + head + " (type " + head.getClass() + ")");
                    result = Number.divide(result, n);
                    Object nextTail = current.tail();
                    if (!(nextTail instanceof LinkedList<?> next)) break;
                    current = next;
                }
                return result;
            }),               
            new Pair<>("%", (BiFunction<Object, Object, Object>) (x, y) ->
                Number.mod((Number) x, (Number) y)
            ),
            new Pair<>("^", (BiFunction<Object, Object, Object>) (x, y) ->
                Number.pow((Number) x, (Number) y)
            ),
            new Pair<>("<", (BiFunction<Object, Object, String>) (x, y) ->
                Number.lessThan((Number) x, (Number) y) ? "#t" : "#f"
            ),
            new Pair<>(">", (BiFunction<Object, Object, String>) (x, y) ->
                Number.greaterThan((Number) x, (Number) y) ? "#t" : "#f"
            ),
            new Pair<>("=", (BiFunction<Object, Object, String>) (x, y) -> {
                if (x instanceof Number nx && y instanceof Number ny)
                    return Number.numericEquals(nx, ny) ? "#t" : "#f";
                if (x instanceof String sx && y instanceof String sy)
                    return sx.equals(sy) ? "#t" : "#f";
                if (x instanceof LinkedList<?> lx && y instanceof LinkedList<?> ly)
                    return lx.equals(ly) ? "#t" : "#f";
                return "#f";
            }),
            new Pair<>("real", (Function<Number,Number>) (z) -> {
                if (z.type == Number.Type.COMPLEX || z.type == Number.Type.QUATERNION){
                    return z.real;
                } else {
                    return z;
                }
            }),
            new Pair<>("imaginary", (Function<Number,Number>) (z) -> {
                if (z.type == Number.Type.COMPLEX) {
                    return z.ipart;
                } else {
                    return Number.ZERO_INT;
                }
            }),
            new Pair<>("complex-magnitude", (Function<Number,Number>) (z) -> {
                if (z.type == Number.Type.COMPLEX) {
                    return Number.real(Math.sqrt(Math.pow(Number.toDouble(z.real),2) + Math.pow(Number.toDouble(z.ipart),2)));
                }else{
                  return z;
                }
            }),
            new Pair<>("floor", (Function<Number,Number>) (x) -> {
                if (x.type == Number.Type.INT) {
                    return x;
                }
                if (x.type == Number.Type.RATIONAL) {
                    return Number.integer((int) Math.floor((Number.toInexact(x)).floatVal));
                }
                if (x.type == Number.Type.FLOAT) {
                    return Number.integer((int) Math.floor(x.floatVal));
                }
                else{
                    return Number.ZERO_INT;
                }
            }),
            new Pair<>("eq?", (Function<LinkedList<?>, Object>) (args) -> {
                if (args == null || args.head() == null)
                    return "#f";
                // eq? must take exactly two arguments
                LinkedList<?> rest = (LinkedList<?>) args.tail();
                if (rest == null || rest.head() == null)
                    throw new SyntaxException("eq? expects 2 arguments");
                Object a = args.head();
                Object b = rest.head();
                if (a == b) return "#t";
                if (a == null || b == null) return "#f";
                if (a instanceof Number && b instanceof Number)
                    return Number.numericEquals((Number) a, (Number) b) ? "#t" : "#f";
                if (a instanceof BigInteger && b instanceof BigInteger)
                    return ((BigInteger) a).equals(b) ? "#t" : "#f";
                if (a instanceof String && b instanceof String)
                    return ((String) a).equals(b) ? "#t" : "#f";
                if (a instanceof Symbol sa && b instanceof Symbol sb)
                    return sa.name.equals(sb.name) ? "#t" : "#f";
                if (a instanceof LinkedList<?> la && b instanceof LinkedList<?> lb)
                    return la.equals(lb) ? "#t" : "#f";
                return "#f";
            })
        );
        // evalString("(define chars->string (lambda (chars) (foldl (lambda (acc ch) (cons ch acc)) \"\" (reverse chars))))", environment);
        return environment;
    }
    private static String unescapeJava(String s) {
        // Convert common ANSI escape encodings first
        s = s.replace("\\u001b", "\u001b").replace("\\033", "\u001b");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                switch (next) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case '"' -> sb.append('"');
                    case '\'' -> sb.append('\'');
                    case '\\' -> sb.append('\\');
                    case 'u' -> {
                        // Unicode escape: \\uXXXX
                        if (i + 4 < s.length()) {
                            String hex = s.substring(i + 1, i + 5);
                            try {
                                int code = Integer.parseInt(hex, 16);
                                sb.append((char) code);
                                i += 4;
                            } catch (NumberFormatException e) {
                                sb.append("\\u").append(hex);
                            }
                        } else {
                            sb.append("\\u");
                        }
                    }
                    default -> sb.append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
