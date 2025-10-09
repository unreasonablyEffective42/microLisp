import java.util.Scanner;
import java.util.ArrayList;

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
    int ind = indent(toks); 
    System.out.println(" ".repeat(ind) + '|');
  }
  public static ArrayList<Token> openToks(String src){
    Lexer lexer = new Lexer(src);
    Stack toks = new Stack<Token>();
    int lpars = 0;
    int rpars = 0;
    Token current = lexer.getNextToken();
    String type = "";
    while (!current.type().equals("EOF")){
      type = (String) current.type();
      switch (type){
        case "LPAREN":
          lpars++;
          break;
        case "RPAREN":
          rpars++;
          break;
        default:
          break;
      }
      toks.push(current);
      current = lexer.getNextToken();
    } 
    current = (Token) toks.peek();
    int mismatch = 0;
    do { 
      type = (String) current.type();
      switch (type){
        case "LPAREN":
          mismatch--;
          break;
        case "RPAREN":
          mismatch++; 
          break;
        default:
          break;
      } 
      Token discard = (Token) toks.pop();
      current = (Token) toks.peek();
    } while(mismatch > 0);
    ArrayList<Token> ret = new ArrayList<Token>();
    while (toks.size > 0){
      ret.add((Token) toks.pop());
    }
    return ret;

  }
  public static int indent(ArrayList<Token> openToks){
    int ret = 0;
    for (int i = 0; i < openToks.size(); i++){
      String type = (String) openToks.get(i).type();
      switch (type) {
        case "RPAREN":
        case "LPAREN":
          ret++;
          break;
        case "LET":
          ret += 4;
          break;
        case "COND":
          ret += 5;
          break;
        case "SYMBOL":
          ret += ((String)openToks.get(i).value()).length();
          break;
        case "NUMBER":
          ret += openToks.get(i).value().toString().length();
          break;
        default:
          ret ++;
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
