import java.util.function.Function;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class Vector{
    public Object[] elems;
    public int size;
     
    Vector(Object[] nums){
        elems = nums;
        size = nums.length;
    }
    Vector(LinkedList nums){
        this.size = nums.size();
        LinkedList current = nums;
        ArrayList temp = new ArrayList();
        while(current != null){
            temp.add(current.head());
            current = (LinkedList)current.tail();
        }
        elems = temp.toArray();
    }

    Vector(Vector other) {
        this.elems = Arrays.copyOf(other.elems, other.size);
        this.size  = other.size;
    }


    public static Vector of (Object[] nums){
       return new Vector(nums);
    }

    public static void addVectorEnv(Environment env){
        env.addFrame(
            new Pair<>("size", (Function<Vector, Number>) (vect) -> {
                return Number.integer(vect.size);
            }),
            new Pair<>("vector", (Function<LinkedList, Vector>) (elems) -> {
                return new Vector(elems);
            })
        );
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("<");
        for (int i = 0; i < size; i++){
            str.append(elems[i].toString());
            if (i < size-1) {
                str.append(" ");
            } 
        }
        str.append(">");
        return str.toString();
    }
}

