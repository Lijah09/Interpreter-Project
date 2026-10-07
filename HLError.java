public class HLError extends RuntimeException {
    public final int line;

    public HLError(String message, int line) {
        super("Line " + line + ": " + message);
        this.line = line;
    }
}
