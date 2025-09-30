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
    stack = stack.tail();  
    return res; 
  }

  public void push(T elem){
    stack = new LinkedList(elem, stack);
  }
}
