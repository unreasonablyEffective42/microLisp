import java.util.Scanner;

public class ParenCounter{
  public static void main(String[] args){
    Scanner sc = new Scanner(System.in);
    String src = "(let ((x 0)";
    System.out.println(src);
    System.out.printf(tabs(src,lParens(src)));
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
