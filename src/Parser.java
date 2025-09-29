import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/*
The Parser is an object that has a lexer. It consumes tokens one at a time from the lexer and builds
the abstract syntax tree using our Node class. It uses a recursive descent algorithm.
 */
public class Parser {
    Lexer lexer;
    Token eof = new Token("EOF", null);
    List keywords = Arrays.asList("COND", "QUOTE", "LAMBDA", "SYMBOL");
    public Parser(String src) {
        this.lexer = new Lexer(src);
    }
    //The recursive descent method, it categorizes the types of tokens and uses that information to construct the tree
    public Node<Token> parse() {
        //Get the first token for this recursive call
        Token current = lexer.getNextToken();
        //add an EOF token to the tree, will end the parsing operation
        if (current.equals(eof)) {
            Node<Token> node = new Node<Token>(eof);
            return node;
        }
        //If the current token is a LPAREN, begin the process of creating a new s-expression,
        //this node be completed and returned to the caller when a RPAREN is detected.
        else if (current.type().equals("LPAREN")) {
            current = lexer.getNextToken();
            //Unmatched RPAREN after a LPAREN, throw error
            if (current.equals(eof)) {
                throw new SyntaxException("Unexpected EOF encountered: '(' not matched with ')'");
            }
            //return the null token: ()
            if (current.type().equals("RPAREN")) {
                Node<Token> node = new Node<>(new Token("NULL", "()"));
                return node;
            }
            //If the source is syntactically correct, only symbols or keywords will immediately follow a LPAREN
            else if (keywords.contains(current.type())) {
                //This is the node that will be returned by this level of the recursive parse, its value is either
                //an operation, or a label, which could be a function
                Node<Token> node = new Node<>(current);
                current = lexer.getNextToken();
                //This level of parsing will end when we reach a RPAREN
                while (!current.type().equals("RPAREN")) {
                    /*If we get another LPAREN during this level of parsing, we will back up the lexer,
                      and then recursively start a new parsing operation which will eventually return a node
                      which may itself have child nodes if more recursive calls were made
                     */
                    if (current.type().equals("LPAREN")) {
                        lexer.backUp();
                        node.addChild(this.parse());
                        current = lexer.getNextToken();
                    }
                    //add the following LABEL and NUMBER type tokens as children of the current node,
                    //Labels, if they end up as bound variables in an environment will be evaluated for their values
                    // in the evaluator
                    else if (current.type().equals("SYMBOL") || current.type().equals("NUMBER") || current.type().equals("BOOLEAN") || current.type().equals("CHARACTER") || current.type().equals("STRING")) {
                        node.createChild(current);
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
