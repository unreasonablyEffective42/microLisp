import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.math.BigInteger;
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
    List<Character> parsableSymbols = Arrays.asList('-','+','*','%','!','?','/','|','^','&','$','@','`','\\',':','[',']','_','=','.',',','<','>');
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
            currentChar = '~';
        }
    }
    //backs up the current position and updates to previous character
    public void backUp(){
        pos--;
        currentChar = this.src.charAt(pos);
    }
    //if a numeric character is detected, keep consuming until current character is not a digit,
    //then return a NUMBER token with the number as a string
    private Token number(){
        StringBuilder res = new StringBuilder();
        while (Character.isDigit(this.currentChar)){
            res.append(currentChar);
            this.advance();
        }
        Token tok = new Token("NUMBER", new BigInteger(res.toString()));
        return tok;
    }
    //This detects contiguous letters and symbols, and creates a string, these could be keywords, variable names
    //or functions. Returns a LABEL token with the label 
    private Token symbol() {
        StringBuilder res = new StringBuilder();
        while (Character.isLetter(this.currentChar) || parsableSymbols.contains(this.currentChar)) {
            res.append(currentChar);
            this.advance();
        }
        String res2 = res.toString();
        // --- special keywords ---
        switch (res2) {
            case "lambda": return new Token("LAMBDA", "");
            case "cond":   return new Token("COND", "");
            case "quote":  return new Token("QUOTE", "");
            case "define": return new Token("DEFINE", "");
            case "list":   return new Token("LIST", "");
            case "do":     return new Token("DO", "");
            case "let":    return new Token("LET", "");
            case "lets":   return new Token("LETS", "");
            case "letr":   return new Token("LETR", "");
            case "eq?":    return new Token("SYMBOL", "eq?"); // eq? now just a symbol
        }

        // --- arithmetic and logical operators as symbols ---
        if ("+-*/%^<>=!".contains(res2)) {
            return new Token("SYMBOL", res2);
        }

        // --- default symbol ---
        return new Token("SYMBOL", res2);
    }
    //Lexing booleans #t,#f, or chars #\c
    private Token special(){
        StringBuilder res = new StringBuilder();
        res.append(currentChar);
        this.advance();
        if (Character.isLetter(this.currentChar)) {
            while (Character.isLetter(this.currentChar)) {
                res.append(currentChar);
                this.advance();
            }
            Token tok = new Token("BOOLEAN", res.toString());
            return tok;
        }else if (currentChar == '\\'){
            res.append(currentChar);
            this.advance();
            while (Character.isLetter(this.currentChar)) {
                res.append(currentChar);
                this.advance();
            }
            Token tok = new Token("CHARACTER", res.toString());
            return tok;
        }
        else {
            return null;
        }

    }    
    private Token string() {
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
    return new Token("STRING", res.toString());}
    //advances past any detected whitespaces
    private void skipWhitespace(){
        while (Character.isWhitespace((this.currentChar))) {
            this.advance();
        }
    }
    private void discardComment() {
        // Advance until newline or EOF sentinel (~)
        while (this.currentChar != '\n' && this.currentChar != '~') {
            this.advance();
        }
        // Optionally skip the newline itself
        if (this.currentChar == '\n') {
            this.advance();
        }
    }
    //This is where the magic happens, depending on what the current character is
    //the conditionals will choose the correct kind of token to produce  
    public Token getNextToken() {
        if (this.currentChar == '~') {
            return new Token<>("EOF", "EOF");
        }

        if (Character.isWhitespace(this.currentChar)) {
            this.skipWhitespace();
            return this.getNextToken();
        }

        if (Character.isDigit(this.currentChar)) {
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
            this.advance();
            return new Token<>("LPAREN", null);
        }
        else if (this.currentChar == ')') {
            this.advance();
            return new Token<>("RPAREN", null);
        }
        else if (this.currentChar == '\'') {
            this.advance();
            return new Token<>("QUOTE", "");
        }
        else if (this.currentChar == '.') {
            this.advance();
            return new Token<>("DOT", ".");
        }
        else if (this.currentChar == ';'){
            this.discardComment();
            return this.getNextToken();
        }

        // --- fallback ---
        throw new SyntaxException("Unexpected character '" + currentChar + "' at position " + pos);
    }
}
