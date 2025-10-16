public class Vector{
    public Object[] elems;
    public int size;
     
    Vector(Object[] nums){
        elems = nums;
        size = nums.length;
    }

    public static Vector of (Object[] nums){
       return new Vector(nums);
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

