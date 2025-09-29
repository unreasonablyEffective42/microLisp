import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/*
The Parser is an object that has a lexer. It consumes tokens one at a time from the lexer and builds
the abstract syntax tree using our Node class. It uses a recursive descent algorithm.
 */
public class Parser {
    Lexer lexer;
    Token eof = new Token("EOF", "EOF");
    List keywords = Arrays.asList("COND", "QUOTE", "LAMBDA", "SYMBOL", "PRIMITIVE", "DEFINE");

    public Parser(String src) {
        this.lexer = new Lexer(src);
    }

    //The recursive descent method, it categorizes the types of tokens and uses that information to construct the tree
    public Node<Token> parse() {
        //Get the first token for this recursive call
        Token current = lexer.getNextToken();
        //add an EOF token to the tree, will end the parsing operation
        if (current.type().equals("EOF")) {
            Node<Token> node = new Node<Token>(new Token<>("EOF","EOF"));
            return node;
        }
        if (current.type().equals("NUMBER") || current.type().equals("BOOLEAN") || current.type().equals("STRING")||current.type().equals("SYMBOL")) {
            Node<Token> node = new Node<>(new Token(current.type(), current.value()));
            return node;
        }
        //If the current token is a LPAREN, begin the process of creating a new s-expression,
        //this node be completed and returned to the caller when a RPAREN is detected.
        else if (current.type().equals("LPAREN")) {
            current = lexer.getNextToken();

            if (current.type().equals("EOF")) {
                throw new SyntaxException("Unexpected EOF encountered: '(' not matched with ')'");
            }
            if (current.type().equals("RPAREN")) {
                return new Node<>(new Token("NULL", "()"));
            }

            // Case 1: operator is a keyword (lambda, define, etc.)
            if (keywords.contains(current.type())) {
                Node<Token> node = new Node<>(current);
                current = lexer.getNextToken();

                // Special handling for lambda: next thing must be a param list
                if (node.getValue().type().equals("LAMBDA")) {
                    if (!current.type().equals("LPAREN")) {
                        throw new SyntaxException("Lambda must be followed by a parameter list in parentheses");
                    }
                    // Parse parameter list
                    Node<Token> paramList = new Node<>(new Token("PARAMS", null));
                    current = lexer.getNextToken();
                    while (!current.type().equals("RPAREN")) {
                        if (!current.type().equals("SYMBOL")) {
                            throw new SyntaxException("Parameter list must contain only symbols, found: " + current);
                        }
                        paramList.createChild(current);
                        current = lexer.getNextToken();
                    }
                    node.addChild(paramList);

                    // Parse body expression
                    node.addChild(this.parse());
                    return node;
                }

                // General case: keep parsing children until RPAREN
                while (!current.type().equals("RPAREN")) {
                    if (current.type().equals("LPAREN")) {
                        lexer.backUp();
                        node.addChild(this.parse());
                        current = lexer.getNextToken();
                    } else if (current.type().equals("SYMBOL") || current.type().equals("NUMBER") ||
                            current.type().equals("BOOLEAN") || current.type().equals("CHARACTER") ||
                            current.type().equals("STRING")) {
                        node.createChild(current);
                        current = lexer.getNextToken();
                    }
                }
                return node;
            }

            // Case 2: operator itself is a subexpression, not a keyword
            else if (current.type().equals("LPAREN")) {
                lexer.backUp();
                Node<Token> node = this.parse(); // parse the operator expr
                current = lexer.getNextToken();
                while (!current.type().equals("RPAREN")) {
                    if (current.type().equals("LPAREN")) {
                        lexer.backUp();
                        node.addChild(this.parse());
                        current = lexer.getNextToken();
                    } else {
                        node.createChild(current);
                        current = lexer.getNextToken();
                    }
                }
                return node;
            }

            // Case 3: operator is a plain symbol (like "foo")
            else if (current.type().equals("SYMBOL")) {
                Node<Token> node = new Node<>(current);
                current = lexer.getNextToken();
                while (!current.type().equals("RPAREN")) {
                    if (current.type().equals("LPAREN")) {
                        lexer.backUp();
                        node.addChild(this.parse());
                        current = lexer.getNextToken();
                    } else {
                        node.createChild(current);
                        current = lexer.getNextToken();
                    }
                }
                return node;
            } else {
                throw new SyntaxException("Unexpected token after '(': " + current);
            }
        }
        else {
            throw new SyntaxException("Unexpected EOF encountered: '(' not matched with ')'");
        }
    }
}