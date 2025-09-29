//Generic token type
public record Token<String, type>(String type, type value) {
    public boolean equals(Token<String, type> other) {
        return (this.type.equals(other.type) && this.value.equals(other.value));
    }
}
