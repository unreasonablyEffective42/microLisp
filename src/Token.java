// Generic token type that can optionally carry source location information.
public record Token<String, T>(String type, T value, int line, int column) {
    public Token(String type, T value) {
        this(type, value, -1, -1);
    }

    public boolean hasLocation() {
        return line >= 1 && column >= 1;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Token<?, ?> tok)) return false;
        Object otherValue = tok.value();
        return this.type.equals(tok.type())
                && (this.value == null ? otherValue == null : this.value.equals(otherValue));
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (value == null ? 0 : value.hashCode());
        return result;
    }
}
