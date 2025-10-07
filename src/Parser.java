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
        Token tok = lexer.getNextToken();
        if (tok.type().equals("LPAREN")) {
            Node<Token> listNode = new Node<>(new Token("LIST",""));
            Token inner = lexer.getNextToken();
            while (!inner.type().equals("RPAREN")) {
                if (inner.type().equals("LPAREN")) {
                    lexer.backUp();
                    listNode.addChild(parseDatum());
                } else {
                    listNode.createChild(inner);
                }
                inner = lexer.getNextToken();
            }
            return listNode;
        } else {
            // atom: number, symbol, string, boolean, etc.
            return new Node<>(tok);
        }
    }
    
    public Node<Token> parse() {
        //Get the first token for this recursive call
        Token current = lexer.getNextToken();
        //add an EOF token to the tree, will end the parsing operation
        if (current.type().equals("EOF")) {
            return new Node<>(new Token<>("EOF","EOF"));
        }
        // atoms: numbers, booleans, strings, symbols
        if (current.type().equals("NUMBER") || current.type().equals("BOOLEAN") ||
            current.type().equals("STRING") || current.type().equals("SYMBOL")) {
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
            current = lexer.getNextToken();

            if (current.type().equals("EOF")) {
                throw new SyntaxException("Unexpected EOF encountered: '(' not matched with ')'");
            }
            if (current.type().equals("RPAREN")) {
                return new Node<>(new Token("LIST", ""));
            }
            // Case 1: operator is a keyword (lambda, define, etc.)
            if (keywords.contains(current.type())) {
                Node<Token> node = new Node<>(current);
                current = lexer.getNextToken();
                // ---------- lambda special form ----------
                if (node.getValue().type().equals("LAMBDA")) {
                    if (!current.type().equals("LPAREN")) {
                        throw new SyntaxException("Lambda must be followed by a parameter list in parentheses");
                    }
                    // Parse parameter list
                   
                    // Parse parameter list (possibly empty)
                    Node<Token> paramList = new Node<>(new Token("PARAMS", null));
                    current = lexer.getNextToken();
                    if (current.type().equals("RPAREN")) {
                        // No parameters at all — fine
                        node.addChild(paramList);
                    } else {
                        while (!current.type().equals("RPAREN")) {
                            if (!current.type().equals("SYMBOL")) {
                                throw new SyntaxException("Parameter list must contain only symbols, found: " + current);
                            }
                            paramList.createChild(current);
                            current = lexer.getNextToken();
                        }
                        node.addChild(paramList);
                    }
                    
                    // Parse body expression
                    node.addChild(this.parse());

                    // NEW: consume the closing ')' of the (lambda …) form
                    Token closer = lexer.getNextToken();
                    if (!closer.type().equals("RPAREN")) {
                        throw new SyntaxException("Lambda must end with ')', found: " + closer);
                    }

                    return node;

                }
                // ---------- quote special form ----------
                if (node.getValue().type().equals("QUOTE")) {
                    if (current.type().equals("LPAREN")) {
                        lexer.backUp();
                        node.addChild(parseDatum());   // parse raw datum inside (quote …)
                        lexer.getNextToken();          // consume closing RPAREN
                        return node;
                    } else {
                        node.addChild(new Node<>(current));
                        return node;
                    }
                }
                // ---------- cond special form ----------
                if (node.getValue().type().equals("COND")) {                    
                    // Parse each clause until the closing RPAREN of cond
                    while (!current.type().equals("RPAREN")) {
                        if (!current.type().equals("LPAREN")) {
                            throw new SyntaxException("cond clauses must be lists, found: " + current);
                        }
                        // Enter clause list
                        current = lexer.getNextToken();
                        Node<Token> clause = new Node<>(new Token("CLAUSE", null));
                        // Parse predicate (allow any expression, including literals like #f or 1)
                        if (current.type().equals("LPAREN")) {
                            lexer.backUp();
                            clause.addChild(this.parse());
                            current = lexer.getNextToken();
                        } else if (current.type().equals("QUOTE")) {
                            clause.addChild(parseDatum());
                            current = lexer.getNextToken();
                        } else {
                            // For literal atoms (NUMBER, BOOLEAN, STRING, SYMBOL, etc.)
                            clause.addChild(new Node<>(current));
                            current = lexer.getNextToken();
                        }

                        // Advance if any trailing whitespace or comments before body
                        while (current.type().equals("WHITESPACE") || current.type().equals("COMMENT")) {
                            current = lexer.getNextToken();
                        }
                                            
                        // Parse body expressions until RPAREN
                        while (!current.type().equals("RPAREN")) {
                            if (current.type().equals("LPAREN")) {
                                lexer.backUp();
                                clause.addChild(this.parse());
                                current = lexer.getNextToken();
                            } else if (current.type().equals("QUOTE")) {
                                clause.addChild(parseDatum());   // handle 'datum (e.g., '())
                                current = lexer.getNextToken();
                            } else {
                                clause.addChild(new Node<>(current));
                                current = lexer.getNextToken();
                            }
                        }
                      // At this point, current == RPAREN for the clause
                      node.addChild(clause);

                      // Advance to next token for the outer cond loop
                      current = lexer.getNextToken();
                  }
                  return node; 
                }
                // ---------- do block form -------- 
                if (node.getValue().type().equals("DO")){ 
                    while (!current.type().equals("RPAREN")) { 
                        if (current.type().equals("LPAREN")){ 
                            lexer.backUp(); 
                            node.addChild(this.parse()); 
                            current = lexer.getNextToken(); 
                        } else if (current.type().equals("QUOTE")) { 
                            node.addChild(parseDatum()); 
                            current = lexer.getNextToken(); 
                        } else if (current.type().equals("SYMBOL") || current.type().equals("NUMBER") 
                                || current.type().equals("BOOLEAN") || current.type().equals("CHARACTER") 
                                || current.type().equals("STRING")) {
                            node.createChild(current); 
                            current = lexer.getNextToken(); 
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
                        current = lexer.getNextToken();
                        if (!current.type().equals("LPAREN")) {
                            throw new SyntaxException("Named let must be followed by a binding list in parentheses");
                        }

                        // Parse binding list
                        Node<Token> bindings = new Node<>(new Token("BINDINGS", null));
                        current = lexer.getNextToken();
                        while (!current.type().equals("RPAREN")) {
                            if (!current.type().equals("LPAREN")) {
                                throw new SyntaxException("each let binding must be enclosed in parentheses");
                            }
                            current = lexer.getNextToken();
                            if (!current.type().equals("SYMBOL")) {
                                throw new SyntaxException("binding must start with a symbol, found: " + current);
                            }
                            Node<Token> pair = new Node<>(new Token("BINDING", null));
                            pair.createChild(current);
                            Node<Token> valueExpr = this.parse();
                            pair.addChild(valueExpr);

                            Token closer = lexer.getNextToken();
                            if (!closer.type().equals("RPAREN")) {
                                throw new SyntaxException("binding must end with ')', found: " + closer);
                            }
                            bindings.addChild(pair);
                            current = lexer.getNextToken();
                        }

                        // After bindings list, parse body
                        Node<Token> body = this.parse();

                        Token closer = lexer.getNextToken();
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
                    current = lexer.getNextToken(); // enter the binding list
                    while (!current.type().equals("RPAREN")) {
                        if (!current.type().equals("LPAREN")) {
                            throw new SyntaxException("each " + node.getValue().type() + " binding must be enclosed in parentheses");
                        }

                        current = lexer.getNextToken();
                        if (!current.type().equals("SYMBOL")) {
                            throw new SyntaxException("binding must start with a symbol, found: " + current);
                        }

                        Node<Token> pair = new Node<>(new Token("BINDING", null));
                        pair.createChild(current);

                        Node<Token> valueExpr = this.parse();
                        pair.addChild(valueExpr);

                        Token closer = lexer.getNextToken();
                        if (!closer.type().equals("RPAREN")) {
                            throw new SyntaxException("binding must end with ')', found: " + closer);
                        }

                        bindings.addChild(pair);
                        current = lexer.getNextToken();
                    }

                    node.addChild(bindings);
                    node.addChild(this.parse());

                    Token closer = lexer.getNextToken();
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
                        current = lexer.getNextToken();
                    } else if (current.type().equals("DOT")) {
                        // Parse dotted pair
                        Node<Token> dotNode = new Node<>(current);
                        Token next = lexer.getNextToken();
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
                        current = lexer.getNextToken();
                        if (!current.type().equals("RPAREN")) {
                            throw new SyntaxException("Dotted pair must end the list");
                        }
                        return node;
                    } else if (current.type().equals("QUOTE")) {
                        node.addChild(parseDatum());
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
    current = lexer.getNextToken();
    while (!current.type().equals("RPAREN")) {
        if (current.type().equals("LPAREN")) {
            lexer.backUp();
            apply.addChild(this.parse());
            current = lexer.getNextToken();
        } else if (current.type().equals("QUOTE")) {
            apply.addChild(parseDatum());
            current = lexer.getNextToken();
        } else {
            apply.createChild(current);
            current = lexer.getNextToken();
        }
    }

    return apply;
}

            // Case 3: operator is a plain symbol
            else if (current.type().equals("SYMBOL")) {
                Node<Token> node = new Node<>(current);
                current = lexer.getNextToken();
                while (!current.type().equals("RPAREN")) {
                    if (current.type().equals("LPAREN")) {
                        lexer.backUp();
                        node.addChild(this.parse());
                        current = lexer.getNextToken();
                    } else if (current.type().equals("DOT")) {
                        // Parse dotted pair
                        Node<Token> dotNode = new Node<>(current);
                        Token next = lexer.getNextToken();
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
                        current = lexer.getNextToken();
                        if (!current.type().equals("RPAREN")) {
                            throw new SyntaxException("Dotted pair must end the list");
                        }
                        return node;
                    } else if (current.type().equals("QUOTE")) {
                        node.addChild(parseDatum());
                        current = lexer.getNextToken();
                    } else {
                        node.createChild(current);
                        current = lexer.getNextToken();
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
                current = lexer.getNextToken();
                while (!current.type().equals("RPAREN")) {
                    if (current.type().equals("LPAREN")) {
                        lexer.backUp();
                        node.addChild(this.parse());
                        current = lexer.getNextToken();
                    } else if (current.type().equals("QUOTE")) {
                        node.addChild(parseDatum());
                        current = lexer.getNextToken();
                    } else {
                        node.createChild(current);
                        current = lexer.getNextToken();
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
                current = lexer.getNextToken();
                while (!current.type().equals("RPAREN")) {
                    if (current.type().equals("LPAREN")) {
                        lexer.backUp();
                        node.addChild(this.parse());
                        current = lexer.getNextToken();
                    } else if (current.type().equals("QUOTE")) {
                        node.addChild(parseDatum());
                        current = lexer.getNextToken();
                    } else {
                        node.createChild(current);
                        current = lexer.getNextToken();
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
}

