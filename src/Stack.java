public class Stack<T>{
  LinkedList<T> stack;
  long size = 0;
  Stack(){}
  
  Stack(T first){
    stack = new LinkedList<T>(first);
    size++;
  }
  
  public long size(){return size;}

  public boolean isEmpty(){
    return stack == null;
  }

  public T peek(){
    if (this.isEmpty()){
      throw new RuntimeException("Empty stack");
    }
    return stack.head();
  }
  @SuppressWarnings("unchecked")
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
    size--;
    return res; 
  }
  //some changes
  public void push(T elem){
    stack = new LinkedList<T>(elem, stack);
    size++;
  }

  public static void main(String[] args){
    Stack<Integer> stk = new Stack<>();
    for (int i =5;i<=15;i++){
      stk.push(i);
    }
    System.out.println(stk.peek());
    System.out.println(stk.pop());
    System.out.println(stk.pop());
    System.out.println(stk.size());

  }
}



