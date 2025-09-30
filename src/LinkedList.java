class Pair<T,V>{
  public T first;
  public V second;
}
public class LinkedList<T>{
  private Pair<T,LinkedList> list;

  LinkedList(){
     list = new Pair<>();
  }
  
  LinkedList(T elem){
    list = new Pair<>();
    list.first = elem;
    list.second = null;
  }

  LinkedList(T elem, LinkedList list_){
    list = new Pair<>();
    list.first = elem;
    list.second = list_;
  } 

  public T head(){
    return this.list.first;
  }

  public LinkedList tail(){
    return this.list.second;
  }
}

