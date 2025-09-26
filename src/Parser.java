import java.util.List;

public class Parser {
    Lexer lexer;
    Token eof = new Token("EOF", null);

    public Parser(String src) {
        this.lexer = new Lexer(src);
    }

    public Node<String> parse() {
        Token current = lexer.getNextToken();
        if (current.equals(eof)) {
            Node<String> node = new Node<String>("EOF");
            return node;
        } else if (current.x().equals("LPAREN")) {
            current = lexer.getNextToken();
            if (current.x().equals(eof)) {
                throw new SyntaxException("Unexpected EOF encountered: '(' not matched with ')'");
            }
            if (current.x().equals("RPAREN")) {
                Node<String> node = new Node<>("()");
                return node;
            } else if (current.x().equals("OPERATOR") || current.x().equals("LABEL")) {
                Node<String> node = new Node<String>((String) current.y());
                current = lexer.getNextToken();
                while (!current.x().equals("RPAREN")) {
                    if (current.x().equals("LPAREN")) {
                        lexer.backUp();
                        node.addChild(this.parse());
                        current = lexer.getNextToken();
                    } else if (current.x().equals("LABEL") || current.x().equals("NUMBER")) {
                        node.createChild(current.y().toString());
                        current = lexer.getNextToken();
                    }
                }
                return node;
            } else {
                throw new SyntaxException("Unknown token" + current);
            }
        }
        else {
            throw new SyntaxException("Unexpected token " + current);
        }
    }
}
