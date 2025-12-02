import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Single lexical frame with a parent link; environments share parent chains instead of copying them.
class Frame {
    final Map<String, Object> bindings;
    final Frame parent;

    Frame(Map<String, Object> bindings, Frame parent) {
        this.bindings = bindings;
        this.parent = parent;
    }
}

public class Environment {
    private Frame head;

    Environment() {
        this.head = null;
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    Environment(Pair<String, Object>... firstFrame) {
        this.head = buildFrame(Arrays.asList(firstFrame), null);
    }

    private Frame buildFrame(List<Pair<String, Object>> bindings, Frame parent) {
        Map<String, Object> map = new HashMap<>(bindings.size());
        for (Pair<String, Object> p : bindings) {
            map.put(p.first, p.second);
        }
        return new Frame(map, parent);
    }

    @SuppressWarnings("unchecked")
    public void addFrame(Pair<String, Object>... bindings) {
        this.head = buildFrame(Arrays.asList(bindings), this.head);
    }

    public void addFrame(List<Pair<String, Object>> bindings) {
        this.head = buildFrame(bindings, this.head);
    }

    // Create a new environment that shares the existing chain; new frames will not mutate the parent.
    public Environment fork() {
        Environment env = new Environment();
        env.head = this.head;
        return env;
    }

    @Override
    public String toString() {
        return "";
    }

    public Optional<Object> lookup(String key) {
        Frame current = head;
        while (current != null) {
            if (current.bindings.containsKey(key)) {
                return Optional.ofNullable(current.bindings.get(key));
            }
            current = current.parent;
        }
        return Optional.empty();
    }
}
