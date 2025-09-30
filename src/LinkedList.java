import java.util.ArrayList;

public class LinkedList<T>{
  private Pair<T,LinkedList> list;

  LinkedList(){
     list = new Pair<>();
  }
  
  LinkedList(T elem){
    list = new Pair<>();
    list.first = elem;
    list.second = new LinkedList();
  }

  LinkedList(T elem, LinkedList list_){
    list = new Pair<>();
    list.first = elem;
    list.second = list_;
  } 
  @SafeVarargs
  LinkedList(T... elems){
    if (elems.length == 0){
      this.list = null;
      return;
    }

    this.list = new Pair<>(elems[0], new LinkedList<>());
    LinkedList<T> current = this.list.second;

    for (int i = 1;i < elems.length;i++){
      current.list = new Pair<>(elems[i], new LinkedList<>());
      current = current.list.second;
    }
  }
  
  LinkedList(ArrayList<T> elems){
    if (elems.size() == 0){
      this.list = null;
      return;
    }

    this.list = new Pair<>(elems.get(0),new LinkedList<>());
    LinkedList<T> current = this.list.second;

    for (int i = 1;i < elems.size();i++){
      current.list = new Pair<>(elems.get(i), new LinkedList<>());
      current = current.list.second;
    }
  }

  @Override
  public String toString(){
    StringBuilder str = new StringBuilder("(");
    LinkedList<T> current = this;
    while(!(current == null) && !(current.head() == null)){
      str.append(current.head() + " ");
      current = current.tail();
    }
    str.deleteCharAt(str.length()-1);
    str.append(")");
    return str.toString();
  }

  public void setHead(T newHead){
    this.list.first = newHead;
  }

  public void setTail(LinkedList lst){
    this.list.second = lst;
  }

  public T head(){
    return this.list.first;
  }

  public LinkedList tail(){
    return this.list.second;
  }
}

