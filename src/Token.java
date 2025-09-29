//Generic token type
public record Token<String, T>(String type, T value) {
    public boolean equals(Token<String, T> other) {
        return (this.type.equals(other.type) && this.value.equals(other.value));
    }
}
