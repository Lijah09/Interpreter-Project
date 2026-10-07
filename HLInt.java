import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class HLInt {
    public static void main(String[] args) throws IOException {
        // 1. Get the file name: argument first, prompt as fallback
        String file;
        if (args.length > 0) {
            file = args[0];
        } else {
            System.out.print("Enter source file name: ");
            file = new Scanner(System.in).nextLine().trim();
        }

        String src;
        try {
            src = Files.readString(Path.of(file));
        } catch (IOException e) {
            System.out.println("Cannot read file: " + file);
            return;
        }

        // 2. NOSPACES.TXT (from the original text)
        Files.writeString(Path.of("NOSPACES.TXT"), src.replaceAll("\\s", ""));

        // 3. Lex, then write RES_SYM.TXT (even if lexing failed partway)
        Lexer lexer = new Lexer(src);
        List<Token> tokens = null;
        HLError error = null;
        try {
            tokens = lexer.tokenize();
        } catch (HLError e) {
            error = e;
        }
        Files.write(Path.of("RES_SYM.TXT"), lexer.resSym);

        // 4. Report (parser and executor get added here later)
        if (error != null) {
            System.out.println("ERROR");
            System.err.println(error.getMessage());   // debug detail, not part of the spec
            return;
        }

        for (Token t : tokens) System.out.println(t);   // TEMPORARY: remove once the parser exists
    }
}
