import java.util.ArrayList;
import java.util.List;

public class Lexer {
    int pos = 0;
    String text;
    Character currentChar;
    static Token eof = new Token("EOF", null);
    Lexer (String text_){
        text = text_;
        currentChar = this.text.charAt(pos);
    }

    private void advance(){
        if (pos < text.length() -1){
            pos++;
            currentChar = this.text.charAt(pos);
        }
        else {
            currentChar = '#';
        }
    }
    public void backUp(){
        pos--;
        currentChar = this.text.charAt(pos);
    }

    private Token number(){
        StringBuilder res = new StringBuilder();
        while (Character.isDigit(this.currentChar)){
            res.append(currentChar);
            this.advance();
        }
        Token tok = new Token("NUMBER", res.toString());
        return tok;
    }

    private Token label(){
        StringBuilder res = new StringBuilder();
        while (Character.isLetter(this.currentChar)){
            res.append(currentChar);
            this.advance();
        }
        Token tok = new Token("LABEL", res.toString());
        return tok;
    }

    private void skipWhitespace(){
        while (Character.isWhitespace((this.currentChar))) {
            this.advance();
        }
    }

    public Token getNextToken(){
        if (this.currentChar == '#'){
            return new Token<>("EOF",null);
        }
        if (Character.isWhitespace(this.currentChar)){
            this.skipWhitespace();
        }
        if (Character.isDigit(this.currentChar)){
            return this.number();
        }
        else if (Character.isLetter(this.currentChar)){
            return this.label();
        }
        else if (Character.isWhitespace(this.currentChar)){
            this.skipWhitespace();
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
            return new Token<>("OPERATOR","PLUS");
        }
        else if (this.currentChar == '-'){
            this.advance();
            return new Token<>("OPERATOR","MINUS");
        }
        else if (this.currentChar == '*'){
            this.advance();
            return new Token<>("OPERATOR","MULTIPLY");
        }
        else if (this.currentChar == '/'){
            this.advance();
            return new Token<>("OPERATOR","DIVIDE");
        }
        else if (this.currentChar == '%'){
            this.advance();
            return new Token<>("OPERATOR","MODULO");
        }
        else if (this.currentChar == '^'){
            this.advance();
            return new Token<>("OPERATOR","EXPONENT");
        }
        else if (this.currentChar == '='){
            this.advance();
            return new Token<>("OPERATOR","EQUALS");
        }
        else if (this.currentChar == '<'){
            this.advance();
            return new Token<>("OPERATOR","LT");
        }
        else if (this.currentChar == '>'){
            this.advance();
            return new Token<>("OPERATOR","GT");
        }
        else if (this.currentChar == '!'){
            this.advance();
            return new Token<>("OPERATOR","NOT");
        }
        else if (this.currentChar == ':'){
            this.advance();
            return new Token<>("OPERATOR","COLON");
        }
        return null;

    }
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
