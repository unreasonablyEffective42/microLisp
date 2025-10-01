import java.util.ArrayList;

public class LinkedList<T> {
  Pair<T, Object> list;

  public LinkedList() {
      this.list = null;
  }

  // Proper list constructor
  public LinkedList(T elem, LinkedList<T> tail) {
      this.list = new Pair<>(elem, tail);
  }

  // Improper list constructor
  public LinkedList(T elem, Object tail) {
      this.list = new Pair<>(elem, tail);
  }

  // Single element list
  public LinkedList(T elem) {
      this.list = new Pair<>(elem, null);
  }

  public T head() {
      return list.first;
  }

  @SuppressWarnings("unchecked")
  public Object tail() {
      return list.second;
  }

  public boolean isEmpty() {
      return list == null;
  }

  @SafeVarargs
  LinkedList(T... elems){
    if (elems.length == 0){
      this.list = null;
      return;
    }

    this.list = new Pair<>(elems[0], new LinkedList<>());
    LinkedList<T> current = (LinkedList<T>) this.list.second;

    for (int i = 1;i < elems.length;i++){
      current.list = new Pair<>(elems[i], new LinkedList<>());
      current = (LinkedList<T>) current.list.second;
    }
  }
  
  LinkedList(ArrayList<T> elems){
    if (elems.size() == 0){
      this.list = null;
      return;
    }

    this.list = new Pair<>(elems.get(0),new LinkedList<>());
    LinkedList<T> current = (LinkedList<T>)this.list.second;

    for (int i = 1;i < elems.size();i++){
      current.list = new Pair<>(elems.get(i), new LinkedList<>());
      current = (LinkedList<T>) current.list.second;
    }
  }

  @Override
  public String toString() {
      if (list == null) return "()";
      StringBuilder sb = new StringBuilder("(");
      Object current = this;

      while (current instanceof LinkedList) {
          LinkedList<?> cell = (LinkedList<?>) current;
          if (cell.list == null) {
            sb.deleteCharAt(sb.length() -1);
            break;
          }
          sb.append(cell.head());
          Object tail = cell.tail();
          if (tail == null) {
              break;
          }
          if (tail instanceof LinkedList) {
              sb.append(" ");
              current = tail;
          } else {
              // dotted pair
              sb.append(" . ").append(tail);
              break;
          }
      }
      sb.append(")");
      return sb.toString();
  }

  public void setHead(T newHead){
    this.list.first = newHead;
  }

  public void setTail(LinkedList lst){
    this.list.second = lst;
  }

  public int size(){
    LinkedList current = this;
    int s = 0;
    while (!(current == null)){
      s++;      
      Object tail = current.tail();
      if (tail instanceof LinkedList) {
          current = (LinkedList<T>) tail; // safe cast
      } else {
          // improper list ends here
          break;
      }
    }
    return s;
  }
  public static void main(String[] args){
    LinkedList lst = new LinkedList<>(1, 2);
    System.out.println(lst);
  }
}

