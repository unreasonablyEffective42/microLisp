import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/*The lexer, or tokenizer, takes in a source file as a string, and produces tokens using the
  getNextToken() method. When the entire source has been consumed, the method will always
  return an EOF token.
 */
public class Lexer {
    //Current position in the source
    int pos = 0;
    //The source code
    String src;
    Character currentChar;
    //for comparing tokens to the EOF token
    static Token eof = new Token("EOF", "EOF");
    List<Character> parsableSymbols = Arrays.asList('-','+','*','%','!','?','/','|','^','&','$','\\',':','[',']','_','=','.','<','>');
    List<Character> numericSymbols = Arrays.asList('i','j','k','-','+','/','.');
    List<Token> tokens = new ArrayList<>();
    //Constructor
    Lexer (String src_){
        src = src_;
        currentChar = this.src.charAt(pos);
    }
    /*Advances the position by one and sets the new current character.
      If lexer reaches the end of the source, it replaces the current
      character with # which causes getNextToken to return the EOF Token.
    */
    private void advance(){
        if (pos < src.length() -1){
            pos++;
            currentChar = this.src.charAt(pos);
        }
        else {
            pos = src.length();
            currentChar = '~';
        }
    }
    //backs up the current position and updates to previous character
    public void backUp(){
        if (pos == 0) {
            return;
        }
        pos--;
        currentChar = this.src.charAt(pos);
    }

    private int[] lineAndColumn(int index) {
        int line = 1;
        int column = 1;
        int max = Math.min(index, src.length());
        for (int i = 0; i < max; i++) {
            char c = src.charAt(i);
            if (c == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new int[]{line, column};
    }

    private Token makeToken(String type, Object value, int startPos) {
        int[] lc = lineAndColumn(startPos);
        return new Token<>(type, value, lc[0], lc[1]);
    }
    //if a numeric character is detected, keep consuming until current character is not a digit,
    //then return a NUMBER token with the number as a string
    private Token number(){
        int startPos = pos;
        StringBuilder res = new StringBuilder();
        while (Character.isDigit(this.currentChar)||numericSymbols.contains(this.currentChar)){
            res.append(currentChar);
            this.advance();
        }
        Token tok = makeToken("NUMBER", res.toString(), startPos);
        return tok;
    }
    //This detects contiguous letters and symbols, and creates a string, these could be keywords, variable names
    //or functions. Returns a LABEL token with the label 
    private Token symbol() {
        int startPos = pos;
        StringBuilder res = new StringBuilder();
        while (Character.isLetter(this.currentChar) || parsableSymbols.contains(this.currentChar) || Character.isDigit(this.currentChar)) {
            res.append(currentChar);
            this.advance();
        }
        String res2 = res.toString();
        // --- special keywords ---
        switch (res2) {
            case "lambda":         return makeToken("LAMBDA", "", startPos);
            case "cond":           return makeToken("COND", "", startPos);
            case "quote":          return makeToken("QUOTE", "", startPos);
            case "quasi-quote":    return makeToken("QQUOTE", "", startPos);
            case "unquote":        return makeToken("UNQUOTE","", startPos);
            case "unquote-splice": return makeToken("UNQUOTESPLICE","", startPos);
            case "define":         return makeToken("DEFINE", "", startPos);
            case "list":           return makeToken("LIST", "", startPos);
            case "do":             return makeToken("DO", "", startPos);
            case "let":            return makeToken("LET", "", startPos);
            case "lets":           return makeToken("LETS", "", startPos);
            case "letr":           return makeToken("LETR", "", startPos);
            case "eq?":            return makeToken("SYMBOL", "eq?", startPos); // eq? now just a symbol
        }

        // --- arithmetic and logical operators as symbols ---
        if ("+-*/%^<>=!".contains(res2)) {
            return makeToken("SYMBOL", res2, startPos);
        }

        // --- default symbol ---
        return makeToken("SYMBOL", res2, startPos);
    }
    //Lexing booleans #t,#f, or chars #\c
    private Token special(){
        int startPos = pos;
        StringBuilder res = new StringBuilder();
        res.append(currentChar);
        this.advance();
        if (Character.isLetter(this.currentChar)) {
            while (Character.isLetter(this.currentChar)) {
                res.append(currentChar);
                this.advance();
            }
            Token tok = makeToken("BOOLEAN", res.toString(), startPos);
            return tok;
        }else if (currentChar == '\\'){
            res.append(currentChar);
            this.advance();
            while (Character.isLetter(this.currentChar)) {
                res.append(currentChar);
                this.advance();
            }
            Token tok = makeToken("CHARACTER", res.toString(), startPos);
            return tok;
        }
        else {
            return null;
        }

    }    
    private Token string() {
        int startPos = pos;
        this.advance(); // skip the opening quote
        StringBuilder res = new StringBuilder();
        while (this.currentChar != '\"') {
            if (this.currentChar == '\\') {
                this.advance();
                switch (this.currentChar) {
                    case 'n' -> res.append('\n');
                    case 't' -> res.append('\t');
                    case 'r' -> res.append('\r');
                    case '"' -> res.append('"');
                    case '\\' -> res.append('\\');
                    default -> res.append(this.currentChar); // unknown escape — keep literal
                }
            } else {
                res.append(this.currentChar);
            }
            this.advance();
        }

    this.advance(); // consume closing quote
    return makeToken("STRING", res.toString(), startPos);}
    //advances past any detected whitespaces
    private void skipWhitespace(){
        while (Character.isWhitespace((this.currentChar))) {
            this.advance();
        }
    }
    
    private void discardComment() {
        while (pos < src.length() && this.currentChar != '\n' && this.currentChar != '~') {
            this.advance();
        }
        if (this.currentChar == '\n') {
            this.advance();
        }
    }


    // Peek ahead without consuming a token.
    // Restores BOTH character index and currentChar sentinels.
    public Token<?,?> peekNextToken() {
        int oldPos = this.pos;
        Character oldCh = this.currentChar;
        Token<?,?> tok = this.getNextToken();
        this.pos = oldPos;
        this.currentChar = oldCh;
        return tok;
    }
    //This is where the magic happens, depending on what the current character is
    //the conditionals will choose the correct kind of token to produce  
    public Token getNextToken() {
        if (this.currentChar == '~') {
            return makeToken("EOF", "EOF", pos);
        }

        if (Character.isWhitespace(this.currentChar)) {
            this.skipWhitespace();
            return this.getNextToken();
        }

        if (Character.isDigit(this.currentChar) || this.currentChar == '-' && Character.isDigit(this.src.charAt(pos+1))) {
            return this.number();
        }

        else if (Character.isLetter(this.currentChar) || parsableSymbols.contains(this.currentChar)) {
            return this.symbol();
        }

        else if (this.currentChar == '\"') {
            return this.string();
        }
        else if (this.currentChar == '#') {
            return this.special();
        }
        else if (this.currentChar == '(') {
            int startPos = pos;
            this.advance();
            return makeToken("LPAREN", null, startPos);
        }
        else if (this.currentChar == ')') {
            int startPos = pos;
            this.advance();
            return makeToken("RPAREN", null, startPos);
        }
        else if (this.currentChar == '\'') {
            int startPos = pos;
            this.advance();
            return makeToken("QUOTE", "", startPos);
        }
        else if (this.currentChar == '`') {
            int startPos = pos;
            this.advance();
            return makeToken("QQUOTE", "", startPos);
        }
        else if (this.currentChar == ',') {
            int startPos = pos;
            this.advance();
            if (this.currentChar == '@') {
                this.advance();
                return makeToken("UNQUOTESPLICE","", startPos);
            }
            return makeToken("UNQUOTE", "", startPos);
        } 
        else if (this.currentChar == '.') {
            int startPos = pos;
            this.advance();
            return makeToken("DOT", ".", startPos);
        }
        else if (this.currentChar == ';') {
            // Discard everything after ';' until newline
            this.discardComment();
            // After discarding, also skip any trailing whitespace or newlines
            this.skipWhitespace();
            // Then continue lexing
            return this.getNextToken();
        }

        // --- fallback ---
        int[] lc = lineAndColumn(pos);
        throw new SyntaxException("Unexpected character '" + currentChar + "' at line " + lc[0] + ", column " + lc[1]);
    }
}
