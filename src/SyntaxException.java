//Custom exception for Syntax errors
public class SyntaxException extends RuntimeException {
    public SyntaxException(String message) {
        super(message);
    }

    public SyntaxException(String message, Token<?, ?> token) {
        super(message + locationSuffix(token));
    }

    private static String locationSuffix(Token<?, ?> token) {
        if (token instanceof Token<?, ?> tok && tok.hasLocation()) {
            return " at line " + tok.line() + ", column " + tok.column();
        }
        return "";
    }
}
