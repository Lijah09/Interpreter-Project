import java.util.*;

public class Lexer {
    private static final Set<String> RESERVED = Set.of("integer", "double", "output", "if");
    private static final String[] TWO_CHAR = { ":=", "==", "!=", "<<" };
    private static final String ONE_CHAR = ":;+-<>()";

    private final String src;
    private int pos = 0;
    private int line = 1;

    // Entries for RES_SYM.TXT, filled in as we scan
    public final List<String> resSym = new ArrayList<>();

    public Lexer(String src) { this.src = src; }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (true) {
            skipWhitespace();
            if (pos >= src.length()) break;
            char c = src.charAt(pos);
            if (Character.isLetter(c) || c == '_') tokens.add(word());
            else if (Character.isDigit(c))         tokens.add(number());
            else if (c == '"')                     tokens.add(string());
            else                                   tokens.add(symbol());
        }
        tokens.add(new Token(Token.Type.EOF, "", line));
        return tokens;
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            if (src.charAt(pos) == '\n') line++;
            pos++;
        }
    }

    private Token word() {
        int start = pos;
        while (pos < src.length()
                && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) pos++;
        String text = src.substring(start, pos);
        String lower = text.toLowerCase();
        if (RESERVED.contains(lower)) {
            resSym.add(lower + "\treserved word");
            return new Token(Token.Type.RESERVED, lower, line);   // normalized to lowercase
        }
        return new Token(Token.Type.IDENT, text, line);           // variables keep their case
    }

    private Token number() {
        int start = pos;
        while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        boolean isDouble = false;
        if (pos < src.length() && src.charAt(pos) == '.') {
            pos++;
            int fracStart = pos;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            int decimals = pos - fracStart;
            if (decimals == 0) throw new HLError("digits expected after decimal point", line);
            if (decimals > 2)  throw new HLError("at most 2 decimal places allowed", line);
            isDouble = true;
        }
        String text = src.substring(start, pos);
        return new Token(isDouble ? Token.Type.DOUBLE : Token.Type.INT, text, line);
    }

    private Token string() {
        pos++;                                   // skip opening quote
        int start = pos;
        while (pos < src.length() && src.charAt(pos) != '"') {
            if (src.charAt(pos) == '\n') throw new HLError("unterminated string", line);
            pos++;
        }
        if (pos >= src.length()) throw new HLError("unterminated string", line);
        String text = src.substring(start, pos);
        pos++;                                   // skip closing quote
        return new Token(Token.Type.STRING, text, line);
    }

    private Token symbol() {
        // Check two-character symbols FIRST (longest match)
        for (String s : TWO_CHAR) {
            if (src.startsWith(s, pos)) {
                pos += 2;
                resSym.add(s + "\tsymbol");
                return new Token(Token.Type.SYMBOL, s, line);
            }
        }
        char c = src.charAt(pos);
        if (ONE_CHAR.indexOf(c) >= 0) {
            pos++;
            resSym.add(c + "\tsymbol");
            return new Token(Token.Type.SYMBOL, String.valueOf(c), line);
        }
        throw new HLError("illegal character '" + c + "'", line);
    }
}
