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
    int line = 1;
    int column = 1;
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
        if (src.isEmpty()) {
            currentChar = '~';
        } else {
            currentChar = this.src.charAt(pos);
        }
    }
    /*Advances the position by one and sets the new current character.
      If lexer reaches the end of the source, it replaces the current
      character with # which causes getNextToken to return the EOF Token.
    */
    private void advance(){
        if (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        if (pos < src.length() - 1){
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
        if (currentChar == '\n') {
            line = Math.max(1, line - 1);
            int idx = pos - 1;
            int col = 1;
            while (idx >= 0 && src.charAt(idx) != '\n') {
                col++;
                idx--;
            }
            column = col;
        } else {
            column = Math.max(1, column - 1);
        }
    }

    private Token makeToken(String type, Object value, int startLine, int startCol) {
        return new Token<>(type, value, startLine, startCol);
    }
    //if a numeric character is detected, keep consuming until current character is not a digit,
    //then return a NUMBER token with the number as a string
    private Token number(){
        int startLine = line;
        int startCol = column;
        StringBuilder res = new StringBuilder();
        while (Character.isDigit(this.currentChar)||numericSymbols.contains(this.currentChar)){
            res.append(currentChar);
            this.advance();
        }
        Token tok = new Token<>("NUMBER", res.toString(), startLine, startCol);
        return tok;
    }
    //This detects contiguous letters and symbols, and creates a string, these could be keywords, variable names
    //or functions. Returns a LABEL token with the label 
    private Token symbol() {
        int startLine = line;
        int startCol = column;
        StringBuilder res = new StringBuilder();
        while (Character.isLetter(this.currentChar) || parsableSymbols.contains(this.currentChar) || Character.isDigit(this.currentChar)) {
            res.append(currentChar);
            this.advance();
        }
        String res2 = res.toString();
        // --- special keywords ---
        switch (res2) {
            case "lambda":         return makeToken("LAMBDA", "", startLine, startCol);
            case "cond":           return makeToken("COND", "", startLine, startCol);
            case "quote":          return makeToken("QUOTE", "", startLine, startCol);
            case "quasi-quote":    return makeToken("QQUOTE", "", startLine, startCol);
            case "unquote":        return makeToken("UNQUOTE","", startLine, startCol);
            case "unquote-splice": return makeToken("UNQUOTESPLICE","", startLine, startCol);
            case "define":         return makeToken("DEFINE", "", startLine, startCol);
            case "list":           return makeToken("LIST", "", startLine, startCol);
            case "do":             return makeToken("DO", "", startLine, startCol);
            case "let":            return makeToken("LET", "", startLine, startCol);
            case "lets":           return makeToken("LETS", "", startLine, startCol);
            case "letr":           return makeToken("LETR", "", startLine, startCol);
            case "eq?":            return makeToken("SYMBOL", "eq?", startLine, startCol); // eq? now just a symbol
        }

        // --- arithmetic and logical operators as symbols ---
        if ("+-*/%^<>=!".contains(res2)) {
            return new Token<>("SYMBOL", res2, startLine, startCol);
        }

        // --- default symbol ---
        return new Token<>("SYMBOL", res2, startLine, startCol);
    }
    //Lexing booleans #t,#f, or chars #\c
    private Token special(){
        int startLine = line;
        int startCol = column;
        StringBuilder res = new StringBuilder();
        res.append(currentChar);
        this.advance();
        if (Character.isLetter(this.currentChar)) {
            while (Character.isLetter(this.currentChar)) {
                res.append(currentChar);
                this.advance();
            }
            Token tok = new Token<>("BOOLEAN", res.toString(), startLine, startCol);
            return tok;
        }else if (currentChar == '\\'){
            res.append(currentChar);
            this.advance();
            while (Character.isLetter(this.currentChar)) {
                res.append(currentChar);
                this.advance();
            }
            Token tok = new Token<>("CHARACTER", res.toString(), startLine, startCol);
            return tok;
        }
        else {
            return null;
        }

    }    
    private Token string() {
        int startLine = line;
        int startCol = column;
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
    return new Token<>("STRING", res.toString(), startLine, startCol);}
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
        int oldLine = this.line;
        int oldCol = this.column;
        Token<?,?> tok = this.getNextToken();
        this.pos = oldPos;
        this.currentChar = oldCh;
        this.line = oldLine;
        this.column = oldCol;
        return tok;
    }
    //This is where the magic happens, depending on what the current character is
    //the conditionals will choose the correct kind of token to produce  
    public Token getNextToken() {
        if (this.currentChar == '~') {
            return makeToken("EOF", "EOF", line, column);
        }

        if (Character.isWhitespace(this.currentChar)) {
            this.skipWhitespace();
            return this.getNextToken();
        }

        if (Character.isDigit(this.currentChar) || (this.currentChar == '-' && pos + 1 < src.length() && Character.isDigit(this.src.charAt(pos+1)))) {
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
            int startLine = line;
            int startCol = column;
            this.advance();
            return makeToken("LPAREN", null, startLine, startCol);
        }
        else if (this.currentChar == ')') {
            int startLine = line;
            int startCol = column;
            this.advance();
            return makeToken("RPAREN", null, startLine, startCol);
        }
        else if (this.currentChar == '\'') {
            int startLine = line;
            int startCol = column;
            this.advance();
            return makeToken("QUOTE", "", startLine, startCol);
        }
        else if (this.currentChar == '`') {
            int startLine = line;
            int startCol = column;
            this.advance();
            return makeToken("QQUOTE", "", startLine, startCol);
        }
        else if (this.currentChar == ',') {
            int startLine = line;
            int startCol = column;
            this.advance();
            if (this.currentChar == '@') {
                this.advance();
                return makeToken("UNQUOTESPLICE","", startLine, startCol);
            }
            return makeToken("UNQUOTE", "", startLine, startCol);
        } 
        else if (this.currentChar == '.') {
            int startLine = line;
            int startCol = column;
            this.advance();
            return makeToken("DOT", ".", startLine, startCol);
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
        throw new SyntaxException("Unexpected character '" + currentChar + "' at line " + line + ", column " + column);
    }
}
