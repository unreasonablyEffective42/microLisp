public class Stack<T>{
  LinkedList<T> stack;
  
  Stack(){}
  
  Stack(T first){
    stack = new LinkedList(first); 
  }
  
  public boolean isEmpty(){
    return stack == null;
  }

  public T peek(){
    if (this.isEmpty()){
      throw new RuntimeException("Empty stack");
    }
    return stack.head();
  }

  public T pop(){
    if (this.isEmpty()){
      throw new RuntimeException("Empty stack");
    }
    T res = stack.head();
    Object tail = stack.tail();
    Object t = stack.tail();
    if (!(t instanceof LinkedList)) {
        throw new RuntimeException("Stack corrupted: improper tail");
    }
    stack = (LinkedList<T>) t;
    return res; 
  }
//some changes
  public void push(T elem){
    stack = new LinkedList(elem, stack);
  }
}
