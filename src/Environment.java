import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
//A dictionary class, will be used for environment frames, where symbols will be bound to expressions
//An environment will be a List<Frame>, when a new lexical scope is encountered, the current environment will
// be copied and prepended  to the list, then the bindings in the current frame will be added
//if a symbol already has a binding in the frame, the old value will be overwritten, but since it will just be for
//that frame, when exiting the lexical scope, that frame will be popped off and the previous frame will become the
//new current environment
class Frame{
    List<Pair<String,Object>> bindings;
    Frame (List<Pair<String, Object>> bindings_){
        this.bindings =  bindings_;
    }
    boolean contains(String key){
        for(Pair<String, Object> pair : bindings){
            if (pair.first.equals(key)){
                return true;
            }
        }
        return false;
    }
    Object get(String key){
        for(Pair<String, Object> pair : bindings){
            if (pair.first.equals(key)){
                return pair.second;
            }
        }
        return null;
    }
}

public class Environment {
    ArrayList<Frame> frames= new ArrayList<>();
    @SafeVarargs
    @SuppressWarnings("varargs")
    Environment(Pair<String, Object> ... firstFrame){
        frames.add(new Frame(Arrays.asList(firstFrame)));
    }
    @SuppressWarnings("unchecked")
    public void addFrame(Pair<String,Object> ... bindings){
        frames.add(0,new Frame(Arrays.asList(bindings)));
    }
    public void addFrame(List<Pair<String,Object>> bindings){
        frames.add(0,new Frame(bindings));
    }

    @Override 
    public String toString(){
        return "";
    }

    public Optional<Object> lookup(String key){
        for (Frame frame : frames){
            if (frame.contains(key)){
                return Optional.ofNullable(frame.get(key));
            }
        }
        return Optional.empty();
    }
}


