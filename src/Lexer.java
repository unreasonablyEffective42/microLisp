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
    static Token eof = new Token("EOF", null);
    List<Character> parsableSymbols = Arrays.asList('-','+','*','!','?','/','|','^','&','$','@','`','\\',':','[',']','_','=','.',',');
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
        Token tok = new Token("NUMBER", Integer.valueOf(res.toString()));
        return tok;
    }
    //This detects contiguous letters and symbols, and creates a string, these could be keywords, variable names
    //or functions. Returns a LABEL token with the label
    private Token symbol(){
        StringBuilder res = new StringBuilder();
        while (Character.isLetter(this.currentChar) || parsableSymbols.contains(this.currentChar)){
            res.append(currentChar);
            this.advance();
        }
        String res2 = res.toString();
        if (res2.equals("lambda")){
            Token tok = new Token("LAMBDA", "");
            return tok;
        }
        else if (res2.equals("cond")){
            Token tok = new Token("COND", "");
            return tok;
        }
        else if (res2.equals("quote")){
            Token tok = new Token("QUOTE", "");
            return tok;
        }
        else if (res2.equals("define")){
            Token tok = new Token("DEFINE", "");
            return tok;
        }
        else if (res2.equals("eq?")){
            Token tok = new Token("PRIMITIVE", "EQ");
            return tok;
        }

        else {
            Token tok = new Token("SYMBOL", res.toString());
            return tok;
        }
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

    private Token string(){
        this.advance();
        StringBuilder res = new StringBuilder();
        while (!(this.currentChar == '\"')){
            res.append(currentChar);
            this.advance();
        }
        this.advance();
        return new Token("STRING", res.toString());
    }
    //advances past any detected whitespaces
    private void skipWhitespace(){
        while (Character.isWhitespace((this.currentChar))) {
            this.advance();
        }
    }

    //This is where the magic happens, depending on what the current character is
    //the conditionals will choose the correct kind of token to produce
    public Token getNextToken(){
        if (this.currentChar == '~'){
            return new Token<>("EOF",null);
        }
        if (Character.isWhitespace(this.currentChar)){
            this.skipWhitespace();
        }
        if (Character.isDigit(this.currentChar)){
            return this.number();
        }
        else if (Character.isLetter(this.currentChar)){
            return this.symbol();
        }
        else if (this.currentChar == '\"'){
            return this.string();
        }
        else if (this.currentChar == '#'){
            return this.special();
        }
        else if (this.currentChar == '('){
            this.advance();
            return new Token<>("LPAREN",null);
        }
        else if (this.currentChar == ')'){
            this.advance();
            return new Token<>("RPAREN",null);
        }
        else if (this.currentChar == '+'){
            this.advance();
            return new Token<>("PRIMITIVE","PLUS");
        }
        else if (this.currentChar == '-'){
            this.advance();
            return new Token<>("PRIMITIVE","MINUS");
        }
        else if (this.currentChar == '*'){
            this.advance();
            return new Token<>("PRIMITIVE","MULTIPLY");
        }
        else if (this.currentChar == '/'){
            this.advance();
            return new Token<>("PRIMITIVE","DIVIDE");
        }
        else if (this.currentChar == '%'){
            this.advance();
            return new Token<>("PRIMITIVE","MODULO");
        }
        else if (this.currentChar == '^'){
            this.advance();
            return new Token<>("PRIMITIVE","EXPONENT");
        }
        else if (this.currentChar == '='){
            this.advance();
            return new Token<>("PRIMITIVE","EQUALS");
        }
        else if (this.currentChar == '<'){
            this.advance();
            return new Token<>("PRIMITIVE","LT");
        }
        else if (this.currentChar == '>'){
            this.advance();
            return new Token<>("PRIMITIVE","GT");
        }
        else if (this.currentChar == '!'){
            this.advance();
            return new Token<>("PRIMITIVE","NOT");
        }
        else if (this.currentChar == ':'){
            this.advance();
            return new Token<>("PRIMITIVE","COLON");
        }
        return null;

    }
    //for testing purposes, returns a list of all tokens that can be extracted from the source
    public List<Token<String,String>> getTokens(){
        Token currentToken =  this.getNextToken();
        List<Token<String,String>> res = new ArrayList<>();
        if (currentToken == Lexer.eof){
            throw new SyntaxException("File is empty!");
        }
        do{
            res.add(currentToken);
            currentToken = this.getNextToken();
        }while (!currentToken.equals(Lexer.eof));
        return res;
    }
}
