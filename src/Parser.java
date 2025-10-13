import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/*
The Parser is an object that has a lexer. It consumes tokens one at a time from the lexer and builds
the abstract syntax tree using our Node class. It uses a recursive descent algorithm.
 */
public class Parser {
    Lexer lexer;
    Token eof = new Token("EOF", "EOF");
    List keywords = Arrays.asList(
            "COND", "QUOTE", "LAMBDA", 
            "PRIMITIVE", "DEFINE", 
            "LIST", "DO", "LET", "LETS", "LETR");
    boolean quoting = false;
    public Parser(String src) {
        this.lexer = new Lexer(src);
    }
    // Parses a raw datum (used inside quoted expressions).

    private Node<Token> parseDatum() {
        Token tok = normalizeNumberToken(lexer.getNextToken());

        // 'datum at datum level: produce a QUOTE node
        if (tok.type().equals("QUOTE")) {
            return parseQuoted();
        }

        if (tok.type().equals("LPAREN")) {
            Node<Token> listNode = new Node<>(new Token("LIST",""));
            Token inner = normalizeNumberToken(lexer.getNextToken());
            if (inner.type().equals("RPAREN")){
                return listNode;
            }
            while (!inner.type().equals("RPAREN")) {
                if (inner.type().equals("LPAREN")) {
                    lexer.backUp();
                    listNode.addChild(parseDatum()); 
                } else if (inner.type().equals("QUOTE")) {
                    // In datum/list context, 'x should become the literal list (quote x)
                    listNode.createChild(new Token("SYMBOL", "quote")); // first element
                    listNode.addChild(parseDatum());                    // second element is the datum after 'quote'
                } else {
                    listNode.createChild(inner);
                }
                inner = normalizeNumberToken(lexer.getNextToken());
            }
            return listNode;
        } 
        else {
            return new Node<>(tok);
        }

    }
     
    // Handles quote shorthand: 'x, ''x, '''x, etc.  Never uses backUp().
    private Node<Token> parseQuoted() {
        // Count consecutive QUOTE tokens: '''x  => depth = 3
        int depth = 1;
        while (lexer.peekNextToken().type().equals("QUOTE")) {
            normalizeNumberToken(lexer.getNextToken()); // consume the extra ' token
            depth++;
        }

        // After consuming all leading quotes, there MUST be a datum
        Token<?,?> look = lexer.peekNextToken();
        if (look.type().equals("EOF")) {
            throw new SyntaxException("Unexpected EOF while parsing quote");
        }

        // Parse the innermost datum *without* consuming '(' here
        Node<Token> inner;
        if (look.type().equals("LPAREN")) {
            // parseDatum() itself will pull the '(' and read the list datum
            inner = parseDatum();
        } else {
            inner = new Node<>(normalizeNumberToken(lexer.getNextToken()));
        }

        // Wrap it depth times: x -> (quote x) -> (quote (quote x)) -> ...
        for (int i = 0; i < depth; i++) {
            Node<Token> q = new Node<>(new Token<>("QUOTE", ""));
            q.addChild(inner);
            inner = q;
        }
        return inner;
    }

    private Number parseNumber(String raw) {
        String val = raw.trim();
        if (val.isEmpty())
            throw new SyntaxException("Empty numeric literal");

        if (isQuaternionCandidate(val)) {
            return parseQuaternionLiteral(val);
        }

        if (isComplexCandidate(val)) {
            return parseComplexLiteral(val);
        }

        if (isRationalLiteral(val)) {
            return parseRationalLiteral(val);
        }

        if (looksLikeDecimal(val)) {
            return parseDecimalLiteral(val);
        }

        return parseIntegerLiteral(val);
    }

    private boolean isComplexCandidate(String val) {
        int iPos = val.lastIndexOf('i');
        if (iPos == -1) return false;
        if (val.indexOf('j') != -1 || val.indexOf('k') != -1) return false;
        String prefix = val.substring(0, iPos);
        return containsDigit(prefix);
    }

    private boolean isQuaternionCandidate(String val) {
        return val.indexOf('i') != -1 && val.indexOf('j') != -1 && val.indexOf('k') != -1;
    }

    private boolean containsDigit(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }

    private Number parseComplexLiteral(String val) {
        int iPos = val.lastIndexOf('i');
        if (iPos != val.length() - 1)
            throw new SyntaxException("Complex numbers must end with 'i'");

        ComplexParts parts = splitRealImag(val.substring(0, iPos));
        Number real = (parts.real != null) ? parseNumber(parts.real) : Number.integer(0);
        Number imag = parseCoefficientString(parts.imag);
        return Number.complex(real, imag);
    }

    private Number parseQuaternionLiteral(String val) {
        int iPos = val.indexOf('i');
        int jPos = val.indexOf('j', Math.max(iPos + 1, 0));
        int kPos = val.indexOf('k', Math.max(jPos + 1, 0));

        if (iPos == -1 || jPos == -1 || kPos == -1)
            throw new SyntaxException("Invalid quaternion literal: " + val);

        String trailer = val.substring(kPos + 1).trim();
        if (!trailer.isEmpty())
            throw new SyntaxException("Quaternions must end with 'k'");

        ComplexParts prefix = splitRealImag(val.substring(0, iPos));
        Number real = (prefix.real != null) ? parseNumber(prefix.real) : Number.integer(0);
        Number iCoeff = parseCoefficientString(prefix.imag);
        Number jCoeff = parseCoefficientString(val.substring(iPos + 1, jPos));
        Number kCoeff = parseCoefficientString(val.substring(jPos + 1, kPos));
        return Number.quaternion(real, iCoeff, jCoeff, kCoeff);
    }

    private static final class ComplexParts {
        final String real;
        final String imag;
        ComplexParts(String real, String imag) {
            this.real = real;
            this.imag = imag;
        }
    }

    private ComplexParts splitRealImag(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return new ComplexParts(null, "+1");
        }

        int splitIndex = findCoefficientSplitIndex(trimmed);
        if (splitIndex == -1) {
            return new ComplexParts(null, trimmed);
        }

        String real = trimmed.substring(0, splitIndex).trim();
        String imag = trimmed.substring(splitIndex).trim();
        if (!real.isEmpty()) {
            char last = real.charAt(real.length() - 1);
            if (last == '+' || last == '-') {
                real = real.substring(0, real.length() - 1).trim();
                if (real.isEmpty()) {
                    real = null;
                }
            }
        }
        if (real != null && real.isEmpty()) {
            real = null;
        }
        if (imag.isEmpty()) imag = "+1";
        if ("+".equals(real) || "-".equals(real)) {
            real = null;
        }
        return new ComplexParts(real, imag);
    }

    private int findCoefficientSplitIndex(String text) {
        for (int i = text.length() - 1; i > 0; i--) {
            char c = text.charAt(i);
            if (c == '+' || c == '-') {
                return i;
            }
        }
        return -1;
    }

    private Number parseCoefficientString(String segment) {
        String trimmed = segment.trim();
        if (trimmed.isEmpty() || "+".equals(trimmed)) {
            return Number.integer(1);
        }
        if ("-".equals(trimmed)) {
            return Number.integer(-1);
        }
        if (trimmed.startsWith("+-")) {
            trimmed = "-" + trimmed.substring(2).trim();
            if (trimmed.isEmpty() || "+".equals(trimmed)) {
                return Number.integer(-1);
            }
        } else if (trimmed.startsWith("-+")) {
            trimmed = "-" + trimmed.substring(2).trim();
            if (trimmed.isEmpty() || "+".equals(trimmed)) {
                return Number.integer(-1);
            }
        } else if (trimmed.startsWith("++")) {
            trimmed = "+" + trimmed.substring(2).trim();
            if (trimmed.isEmpty() || "+".equals(trimmed)) {
                return Number.integer(1);
            }
        } else if (trimmed.startsWith("--")) {
            trimmed = "-" + trimmed.substring(2).trim();
            if (trimmed.isEmpty() || "+".equals(trimmed)) {
                return Number.integer(-1);
            }
        }
        return parseNumber(trimmed);
    }

    private boolean isRationalLiteral(String val) {
        return val.contains("/") && !val.contains("i") && !val.contains("j") && !val.contains("k");
    }

    private Number parseRationalLiteral(String literal) {
        String inner = literal.trim();
        if (inner.startsWith("+")) {
            inner = inner.substring(1).trim();
        }

        if (isFullyParenthesized(inner)) {
            inner = inner.substring(1, inner.length() - 1).trim();
        }

        int slash = inner.indexOf('/');
        if (slash <= 0 || slash == inner.length() - 1)
            throw new SyntaxException("Invalid rational literal: " + literal);

        String numStr = inner.substring(0, slash).trim();
        String denStr = inner.substring(slash + 1).trim();
        if (numStr.isEmpty() || denStr.isEmpty())
            throw new SyntaxException("Invalid rational literal: " + literal);

        BigInteger numerator;
        BigInteger denominator;
        try {
            numerator = new BigInteger(numStr);
            denominator = new BigInteger(denStr);
        } catch (NumberFormatException e) {
            throw new SyntaxException("Invalid rational literal: " + literal);
        }

        if (denominator.signum() == 0)
            throw new SyntaxException("Division by zero in rational literal: " + literal);

        try {
            long n = numerator.longValueExact();
            long d = denominator.longValueExact();
            return Number.rational(n, d);
        } catch (ArithmeticException ex) {
            return Number.rational(numerator, denominator);
        }
    }

    private boolean isFullyParenthesized(String text) {
        if (text.length() < 2 || text.charAt(0) != '(' || text.charAt(text.length() - 1) != ')')
            return false;
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0 && i != text.length() - 1)
                    return false;
                if (depth < 0)
                    return false;
            }
        }
        return depth == 0;
    }

    private boolean looksLikeDecimal(String val) {
        return val.contains(".") || val.contains("e") || val.contains("E");
    }

    private Number parseDecimalLiteral(String literal) {
        try {
            double d = Double.parseDouble(literal);
            if (!Double.isFinite(d))
                return Number.real(new BigDecimal(literal));
            return Number.real(d);
        } catch (NumberFormatException e) {
            try {
                return Number.real(new BigDecimal(literal));
            } catch (NumberFormatException ex) {
                throw new SyntaxException("Invalid decimal literal: " + literal);
            }
        }
    }

    private Number parseIntegerLiteral(String literal) {
        try {
            return Number.integer(Long.parseLong(literal));
        } catch (NumberFormatException e) {
            try {
                return Number.integer(new BigInteger(literal));
            } catch (NumberFormatException ex) {
                throw new SyntaxException("Invalid integer literal: " + literal);
            }
        }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Token normalizeNumberToken(Token tok) {
        if (tok != null && "NUMBER".equals(tok.type()) && tok.value() instanceof String s) {
            return new Token("NUMBER", parseNumber(s));
        }
        return tok;
    }


    
    public Node<Token> parse() {
        //Get the first token for this recursive call
        Token current = normalizeNumberToken(lexer.getNextToken());
        //add an EOF token to the tree, will end the parsing operation
        if (current.type().equals("EOF")) {
            return new Node<>(new Token<>("EOF","EOF"));
        }
        if (current.type().equals("NUMBER")){
            return new Node<>(current);
        }
        // atoms: numbers, booleans, strings, symbols
        if (current.type().equals("BOOLEAN") || current.type().equals("STRING") || current.type().equals("SYMBOL")) {
            return new Node<>(new Token(current.type(), current.value()));
        }
        // ---------- shorthand quote handling ----------
        else if (current.type().equals("QUOTE")) {
            Node<Token> node = new Node<>(new Token("QUOTE",""));
            node.addChild(parseDatum());   // parse raw datum
            return node;
        }
        // ---------- list / s-expression ----------
        else if (current.type().equals("LPAREN")) {
            current = normalizeNumberToken(lexer.getNextToken());

            if (current.type().equals("EOF")) {
                throw new SyntaxException("Unexpected EOF encountered: '(' not matched with ')'");
            }
            if (current.type().equals("RPAREN")) {
                return new Node<>(new Token("LIST", ""));
            }
            // Case 1: operator is a keyword (lambda, define, etc.)
            if (keywords.contains(current.type())) {
                Node<Token> node = new Node<>(current);
                current = normalizeNumberToken(lexer.getNextToken());
                // ---------- lambda special form ----------
                if (node.getValue().type().equals("LAMBDA")) {
                    if (!current.type().equals("LPAREN")) {
                        throw new SyntaxException("Lambda must be followed by a parameter list in parentheses");
                    }                    
                    // Parse parameter list (possibly empty)
                    Node<Token> paramList = new Node<>(new Token("PARAMS", null));
                    current = normalizeNumberToken(lexer.getNextToken());
                    if (current.type().equals("RPAREN")) {
                        // No parameters at all — fine
                        node.addChild(paramList);
                    } else {
                        while (!current.type().equals("RPAREN")) {
                            if (!current.type().equals("SYMBOL")) {
                                throw new SyntaxException("Parameter list must contain only symbols, found: " + current);
                            }
                            paramList.createChild(current);
                            current = normalizeNumberToken(lexer.getNextToken());
                        }
                        node.addChild(paramList);
                    }                    
                    // Parse body expression
                    node.addChild(this.parse());
                    // NEW: consume the closing ')' of the (lambda …) form
                    Token closer = normalizeNumberToken(lexer.getNextToken());
                    if (!closer.type().equals("RPAREN")) {
                        throw new SyntaxException("Lambda must end with ')', found: " + closer);
                    }
                    return node;
                }

                // ---------- quote special form ----------
                if (node.getValue().type().equals("QUOTE")) {
                    // 'current' is already the first token after QUOTE
                    if (current.type().equals("LPAREN")) {
                        //empty list handling 
                            
                        lexer.backUp();
                        // Parse the quoted datum — may be (), (x), or nested structures
                        Node<Token> datum = parseDatum();

                        // Peek to see if there's a closing ')' for the (quote …)
                        Token<?, ?> maybeCloser = lexer.peekNextToken();
                        if (maybeCloser.type().equals("RPAREN")) {
                            normalizeNumberToken(lexer.getNextToken()); // consume it normally
                        } else if (!maybeCloser.type().equals("EOF")) {
                            throw new SyntaxException("quote: expected ')' to close (quote ...), found: " + maybeCloser);
                        }

                        node.addChild(datum);
                        return node;
                    }

                    // If the datum starts with a shorthand quote again: (quote 'x), (quote ''x), …
                    if (current.type().equals("QUOTE")) {
                        node.addChild(parseQuoted());
                        Token<?, ?> outerClose = lexer.peekNextToken();
                        if (outerClose.type().equals("RPAREN")) {
                            normalizeNumberToken(lexer.getNextToken());
                        } else if (!outerClose.type().equals("EOF")) {
                            throw new SyntaxException("quote: expected ')' to close (quote ...), found: " + outerClose);
                        }
                        return node;
                    }

                    // Atom after QUOTE (symbol/number/string/boolean)
                    node.createChild(current);
                    Token<?, ?> outerClose = lexer.peekNextToken();
                    if (outerClose.type().equals("RPAREN")) {
                        normalizeNumberToken(lexer.getNextToken());
                    } else if (!outerClose.type().equals("EOF")) {
                        throw new SyntaxException("quote: expected ')' to close (quote ...), found: " + outerClose);
                    }
                    return node;
                }
                // ---------- cond special form ----------
                if (node.getValue().type().equals("COND")) {                    
                    // Parse each clause until the closing RPAREN of cond
                    while (!current.type().equals("RPAREN")) {
                        if (!current.type().equals("LPAREN")) {
                            throw new SyntaxException("cond clauses must be lists, found: " + current);
                        }
                        // Enter clause list
                        current = normalizeNumberToken(lexer.getNextToken());
                        Node<Token> clause = new Node<>(new Token("CLAUSE", null));
                        // Parse predicate (allow any expression, including literals like #f or 1)
                        if (current.type().equals("LPAREN")) {
                            lexer.backUp();
                            clause.addChild(this.parse());
                            current = normalizeNumberToken(lexer.getNextToken());
                        } else if (current.type().equals("QUOTE")) {
                            clause.addChild(parseQuoted());
                            current = normalizeNumberToken(lexer.getNextToken());
                        } else {
                            // For literal atoms (NUMBER, BOOLEAN, STRING, SYMBOL, etc.)
                            clause.addChild(new Node<>(current));
                            current = normalizeNumberToken(lexer.getNextToken());
                        }

                        // Advance if any trailing whitespace or comments before body
                        while (current.type().equals("WHITESPACE") || current.type().equals("COMMENT")) {
                            current = normalizeNumberToken(lexer.getNextToken());
                        }                                            
                        // Parse body expressions until RPAREN
                        while (!current.type().equals("RPAREN")) {
                            if (current.type().equals("LPAREN")) {
                                lexer.backUp();
                                clause.addChild(this.parse());
                                current = normalizeNumberToken(lexer.getNextToken());
                            } else if (current.type().equals("QUOTE")) {
                                clause.addChild(parseQuoted());   // handle 'datum (e.g., '())
                                current = normalizeNumberToken(lexer.getNextToken());
                            } else {
                                clause.addChild(new Node<>(current));
                                current = normalizeNumberToken(lexer.getNextToken());
                            }
                        }
                      // At this point, current == RPAREN for the clause
                      node.addChild(clause);

                      // Advance to next token for the outer cond loop
                      current = normalizeNumberToken(lexer.getNextToken());
                  }
                  return node; 
                }
                // ---------- do block form -------- 
                if (node.getValue().type().equals("DO")){ 
                    while (!current.type().equals("RPAREN")) { 
                        if (current.type().equals("LPAREN")){ 
                            lexer.backUp(); 
                            node.addChild(this.parse()); 
                            current = normalizeNumberToken(lexer.getNextToken()); 
                        } else if (current.type().equals("QUOTE")) { 
                            node.addChild(parseQuoted()); 
                            current = normalizeNumberToken(lexer.getNextToken()); 
                        } else if (current.type().equals("SYMBOL") || current.type().equals("NUMBER") 
                                || current.type().equals("BOOLEAN") || current.type().equals("CHARACTER") 
                                || current.type().equals("STRING")) {
                            node.createChild(current); 
                            current = normalizeNumberToken(lexer.getNextToken()); 
                        } 
                    }
                    return node;
                }
                // ---------- LET ----------
                if (node.getValue().type().equals("LET") ||
                    node.getValue().type().equals("LETS") ||
                    node.getValue().type().equals("LETR")) {
                    // --- Named let detection ---
                    if (current.type().equals("SYMBOL")) {
                        Node<Token> nameNode = new Node<>(current); // the let name
                        current = normalizeNumberToken(lexer.getNextToken());
                        if (!current.type().equals("LPAREN")) {
                            throw new SyntaxException("Named let must be followed by a binding list in parentheses");
                        }
                        // Parse binding list
                        Node<Token> bindings = new Node<>(new Token("BINDINGS", null));
                        current = normalizeNumberToken(lexer.getNextToken());
                        while (!current.type().equals("RPAREN")) {
                            if (!current.type().equals("LPAREN")) {
                                throw new SyntaxException("each let binding must be enclosed in parentheses");
                            }
                            current = normalizeNumberToken(lexer.getNextToken());
                            if (!current.type().equals("SYMBOL")) {
                                throw new SyntaxException("binding must start with a symbol, found: " + current);
                            }
                            Node<Token> pair = new Node<>(new Token("BINDING", null));
                            pair.createChild(current);
                            Node<Token> valueExpr = this.parse();
                            pair.addChild(valueExpr);
                            Token closer = normalizeNumberToken(lexer.getNextToken());
                            if (!closer.type().equals("RPAREN")) {
                                throw new SyntaxException("binding must end with ')', found: " + closer);
                            }
                            bindings.addChild(pair);
                            current = normalizeNumberToken(lexer.getNextToken());
                        }
                        // After bindings list, parse body
                        Node<Token> body = this.parse();
                        Token closer = normalizeNumberToken(lexer.getNextToken());
                        if (!closer.type().equals("RPAREN")) {
                            throw new SyntaxException("Named let must end with ')', found: " + closer);
                        }
                        // Wrap as (LET-NAMED name bindings body)
                        Node<Token> namedNode = new Node<>(new Token("LET-NAMED", ""));
                        namedNode.addChild(nameNode);
                        namedNode.addChild(bindings);
                        namedNode.addChild(body);
                        return namedNode;
                    }
                    // ---- regular let parsing ----- 
                    if (!current.type().equals("LPAREN")) {
                        throw new SyntaxException(node.getValue().type() + " must be followed by a binding list in parentheses");
                    }
                    Node<Token> bindings = new Node<>(new Token("BINDINGS", null));
                    current = normalizeNumberToken(lexer.getNextToken()); // enter the binding list
                    while (!current.type().equals("RPAREN")) {
                        if (!current.type().equals("LPAREN")) {
                            throw new SyntaxException("each " + node.getValue().type() + " binding must be enclosed in parentheses");
                        }

                        current = normalizeNumberToken(lexer.getNextToken());
                        if (!current.type().equals("SYMBOL")) {
                            throw new SyntaxException("binding must start with a symbol, found: " + current);
                        }

                        Node<Token> pair = new Node<>(new Token("BINDING", null));
                        pair.createChild(current);

                        Node<Token> valueExpr = this.parse();
                        pair.addChild(valueExpr);

                        Token closer = normalizeNumberToken(lexer.getNextToken());
                        if (!closer.type().equals("RPAREN")) {
                            throw new SyntaxException("binding must end with ')', found: " + closer);
                        }

                        bindings.addChild(pair);
                        current = normalizeNumberToken(lexer.getNextToken());
                    }

                    node.addChild(bindings);
                    node.addChild(this.parse());

                    Token closer = normalizeNumberToken(lexer.getNextToken());
                    if (!closer.type().equals("RPAREN")) {
                        throw new SyntaxException(node.getValue().type() + " must end with ')', found: " + closer);
                    }

                    return node;
                }
                // ---------- general keyword form ----------
                while (!current.type().equals("RPAREN")) {
                    if (current.type().equals("LPAREN")) {
                        lexer.backUp();
                        node.addChild(this.parse());
                        current = normalizeNumberToken(lexer.getNextToken());
                    } else if (current.type().equals("DOT")) {
                        // Parse dotted pair
                        Node<Token> dotNode = new Node<>(current);
                        Token next = normalizeNumberToken(lexer.getNextToken());
                        if (next.type().equals("RPAREN")) {
                            throw new SyntaxException("Dot must be followed by an element");
                        }
                        if (next.type().equals("LPAREN")) {
                            lexer.backUp();
                            dotNode.addChild(this.parse());
                        } else {
                            dotNode.addChild(new Node<>(next));
                        }
                        node.addChild(dotNode);
                        // after parsing dotted cdr, require a closing RPAREN
                        current = normalizeNumberToken(lexer.getNextToken());
                        if (!current.type().equals("RPAREN")) {
                            throw new SyntaxException("Dotted pair must end the list");
                        }
                        return node;
                    } else if (current.type().equals("QUOTE")) {
                        node.addChild(parseQuoted());
                        current = normalizeNumberToken(lexer.getNextToken());
                    } else if (current.type().equals("SYMBOL") || current.type().equals("NUMBER") ||
                              current.type().equals("BOOLEAN") || current.type().equals("CHARACTER") ||
                              current.type().equals("STRING")) {
                        node.createChild(current);
                        current = normalizeNumberToken(lexer.getNextToken());
                    }
                }
                return node;
            }
            
            // Case 2: operator itself is a subexpression — e.g. ((foo 1) 2)
            else if (current.type().equals("LPAREN")) {
                lexer.backUp();

                // Parse the operator expression fully
                Node<Token> opExpr = this.parse();

                // Create an APPLY node to represent (APPLY opExpr arg1 arg2 ...)
                Node<Token> apply = new Node<>(new Token("APPLY", ""));

                // First child is the operator expression itself
                apply.addChild(opExpr);

                // Now gather arguments until closing RPAREN
                current = normalizeNumberToken(lexer.getNextToken());
                while (!current.type().equals("RPAREN")) {
                    if (current.type().equals("LPAREN")) {
                        lexer.backUp();
                        apply.addChild(this.parse());
                        current = normalizeNumberToken(lexer.getNextToken());
                    } else if (current.type().equals("QUOTE")) {
                        apply.addChild(parseQuoted());
                        current = normalizeNumberToken(lexer.getNextToken());
                    } else {
                        apply.createChild(current);
                        current = normalizeNumberToken(lexer.getNextToken());
                    }
                }

                return apply;
            }

            // Case 3: operator is a plain symbol
            else if (current.type().equals("SYMBOL")) {
                Node<Token> node = new Node<>(current);
                current = normalizeNumberToken(lexer.getNextToken());
                while (!current.type().equals("RPAREN")) {
                    if (current.type().equals("LPAREN")) {
                        lexer.backUp();
                        node.addChild(this.parse());
                        current = normalizeNumberToken(lexer.getNextToken());
                    } else if (current.type().equals("DOT")) {
                        // Parse dotted pair
                        Node<Token> dotNode = new Node<>(current);
                        Token next = normalizeNumberToken(lexer.getNextToken());
                        if (next.type().equals("RPAREN")) {
                            throw new SyntaxException("Dot must be followed by an element");
                        }
                        if (next.type().equals("LPAREN")) {
                            lexer.backUp();
                            dotNode.addChild(this.parse());
                        } else {
                            dotNode.addChild(new Node<>(next));
                        }
                        node.addChild(dotNode);
                        // after parsing dotted cdr, require a closing RPAREN
                        current = normalizeNumberToken(lexer.getNextToken());
                        if (!current.type().equals("RPAREN")) {
                            throw new SyntaxException("Dotted pair must end the list");
                        }
                        return node;
                    } else if (current.type().equals("QUOTE")) {
                        node.addChild(parseQuoted());
                        current = normalizeNumberToken(lexer.getNextToken());
                    } else {
                        node.createChild(current);
                        current = normalizeNumberToken(lexer.getNextToken());
                    }
                }

                if (node.getChildren().isEmpty()) {
                    node.addChild(new Node<>(new Token("CALL0","")));
                }
                return node;
            }
            // Case 4: operator is a literal number (list starting with number)
            else if (current.type().equals("NUMBER")) {
                Node<Token> node = new Node<>(new Token<>("LIST",""));
                node.createChild(current);
                current = normalizeNumberToken(lexer.getNextToken());
                while (!current.type().equals("RPAREN")) {
                    if (current.type().equals("LPAREN")) {
                        lexer.backUp();
                        node.addChild(this.parse());
                        current = normalizeNumberToken(lexer.getNextToken());
                    } else if (current.type().equals("QUOTE")) {
                        node.addChild(parseQuoted());
                        current = normalizeNumberToken(lexer.getNextToken());
                    } else {
                        node.createChild(current);
                        current = normalizeNumberToken(lexer.getNextToken());
                    }
                }
                return node;
            }
            
            // Case 5: operator is a literal (boolean, string, or character)
            else if (current.type().equals("BOOLEAN") ||
                    current.type().equals("STRING")  ||
                    current.type().equals("CHARACTER")) {
                Node<Token> node = new Node<>(new Token<>("LIST",""));
                node.createChild(current);
                current = normalizeNumberToken(lexer.getNextToken());
                while (!current.type().equals("RPAREN")) {
                    if (current.type().equals("LPAREN")) {
                        lexer.backUp();
                        node.addChild(this.parse());
                        current = normalizeNumberToken(lexer.getNextToken());
                    } else if (current.type().equals("QUOTE")) {
                        node.addChild(parseQuoted());
                        current = normalizeNumberToken(lexer.getNextToken());
                    } else {
                        node.createChild(current);
                        current = normalizeNumberToken(lexer.getNextToken());
                    }
                }
                return node;
            }
        }
        
        else {
            return parse(); // fallback
        }

        // --- FINAL FALLBACK to satisfy compiler ---
        return new Node<>(new Token<>("EOF","EOF"));
    } // end of parse()

    public static void main(String[] args) {
        String src = "(:: 1 2 3)"; 
        Parser p = new Parser(src);
        Node parsed = p.parse();
        parsed.printNodes(0);
    }

}
