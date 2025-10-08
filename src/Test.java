public class Test {
  public static void main(String[] args){
    String src1 = "(quote (+ 1 2))";
    String src2 = "'(+ 1 2)";
    Parser parse1 = new Parser(src1);
    Parser parse2 = new Parser(src2);
    Node p1 = parse1.parse();
    Node p2 = parse2.parse();
    p1.printNodes(0);
    p2.printNodes(0);
  }
}
