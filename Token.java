public class Token {
    public enum Type { RESERVED, SYMBOL, IDENT, INT, DOUBLE, STRING, EOF }

    public final Type type;
    public final String lexeme;
    public final int line;

    public Token(Type type, String lexeme, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
    }

    public String toString() { return type + "(" + lexeme + ") line " + line; }
}
