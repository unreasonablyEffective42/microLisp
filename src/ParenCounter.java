import java.util.Scanner;
import java.util.ArrayList;
import java.util.Collections;

public class ParenCounter{
  public static void main(String[] args){
    Scanner sc = new Scanner(System.in);
    String src = sc.nextLine();

    /*
    while (true){
      src = sc.nextLine();
      Lexer l = new Lexer(src);
      Token current = l.getNextToken();
      ArrayList<Token> toks =  new ArrayList();
      toks.add(current);
      while (!current.type().equals("EOF")){
        //System.out.println(current);
        current = l.getNextToken();
        toks.add(current);
      }
      System.out.println(toks);
      System.out.println(src);
      System.out.printf(tabs(src,lParens(src)));
    }
    */
    System.out.println(src);
    ArrayList<Token> toks = openToks(src);
    System.out.println(toks);
    int ind = indent(toks); 
    System.out.println(" ".repeat(ind) + '|');
  }


public static ArrayList<Token> openToks(String src){
    Lexer lexer = new Lexer(src);
    Stack toks = new Stack<Token>();

    Token current = lexer.getNextToken();
    while (!current.type().equals("EOF")){
        toks.push(current);
        current = lexer.getNextToken();
    }

    // Walk back from the end, discarding the fully balanced tail.
    current = (Token) toks.peek();
    int mismatch = 0;
    do {
        String type = (String) current.type();
        switch (type){
            case "LPAREN": mismatch--; break;
            case "RPAREN": mismatch++; break;
            default: break;
        }
        Token discard = (Token) toks.pop();
        current = (Token) toks.peek();
    } while (mismatch > 0);

    // Remaining tokens describe the "open path" but are in reverse order (right->left).
    ArrayList<Token> ret = new ArrayList<>();
    while (toks.size > 0){
        ret.add((Token) toks.pop());
    }

    // Make it left->right (outermost -> innermost).
    Collections.reverse(ret);

    // Normalize: replace "( LET|LETS|COND" with "LET|LETS|COND" by dropping the leading LPAREN.
    ArrayList<Token> norm = new ArrayList<>();
    for (int i = 0; i < ret.size(); i++){
        Token t = ret.get(i);
        String type = (String) t.type();
        if ("LPAREN".equals(type) && i + 1 < ret.size()){
            String next = (String) ret.get(i + 1).type();
            if ("LET".equals(next) || "LETS".equals(next) || "COND".equals(next)) {
                // skip this LPAREN; the special form token stands in for the list head
                continue;
            }
        }
        norm.add(t);
    }
    return norm;
}

public static int indent(ArrayList<Token> openToks){
    int ret = 0;

    // Only treat the FIRST (outermost) LET/LETS specially.
    int firstLetIdx  = -1;
    int firstLetsIdx = -1;
    for (int i = 0; i < openToks.size(); i++){
        String t = (String) openToks.get(i).type();
        if (firstLetIdx  < 0 && "LET".equals(t))  firstLetIdx  = i;
        if (firstLetsIdx < 0 && "LETS".equals(t)) firstLetsIdx = i;
    }

    for (int i = 0; i < openToks.size(); i++){
        String type = (String) openToks.get(i).type();
        switch (type){
            case "LPAREN":
            case "RPAREN":
                ret++;
                break;

            case "LET":
                // outermost LET: align just after the '(' that starts the form
                if (i == firstLetIdx) ret++; else ret += 4;  // "let "
                break;

            case "LETS":
                if (i == firstLetsIdx) ret++; else ret += 5; // "lets "
                break;

            case "COND":
                ret += 5; // "cond "
                break;

            case "SYMBOL":
                ret += ((String) openToks.get(i).value()).length() + 1;
                break;

            case "NUMBER":
                ret += openToks.get(i).value().toString().length();
                break;

            default:
                ret++;
                break;
        }
    }
    return ret;
}
    
  public static String tabs(String src,int open){
    int i = src.length() -1;
    int count = 0;
    char current = src.charAt(i);
    StringBuilder res = new StringBuilder(" ");
    if (open > 1){
      do {  
        switch (current){
          case ')':
            count++;
            break;
          case '(':
            count--;
            break;
        } 
        i--;
        current = src.charAt(i);
        res.append(" ");
      } while(count != 0);
      return res.toString();
    }
    return "  ";
  }
  
    public static int lParens(String src){
      int lparens = 0;
      for (int i = 0; i < src.length(); i++){
        switch (src.charAt(i)){
          case '(':
            lparens++;
            break;
          case ')':
            lparens--;
            break;
          default:
            break;
        }
      }
      return lparens;
    }
}
