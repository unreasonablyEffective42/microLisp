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

  public static void main(String[] args){
    Stack stk = new Stack();
    System.out.println(stk.isEmpty());
    stk.push(3);
    stk.push(2);
    stk.push(1);
    System.out.println(stk.isEmpty());
    System.out.println(stk.pop());
    System.out.println(stk.pop());
    System.out.println(stk.pop());
    System.out.println(stk.peek()); 

  }

}
