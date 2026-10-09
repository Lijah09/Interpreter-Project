import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Main program: runs the stages in order  read -> NOSPACES -> lex -> RES_SYM -> parse -> check -> run. */
public class HLInt {
    public static void main(String[] args) throws IOException {
        // 1. Get the file name: command-line argument first, prompt as fallback
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

        // 3. Lexer, then RES_SYM.TXT (written even if lexing failed partway)
        Lexer lexer = new Lexer(src);
        List<Token> tokens = null;
        List<Parser.Statement> program = null;
        HLError error = null;
        try {
            tokens = lexer.tokenize();
        } catch (HLError e) {
            error = e;
        }
        Files.write(Path.of("RES_SYM.TXT"), lexer.resSym);

        // 4. Parser (syntax), then Checker (meaning). Both finish before anything runs.
        if (error == null) {
            try {
                program = new Parser(tokens).parse();
                new Checker().check(program);
            } catch (HLError e) {
                error = e;
            }
        }

        // 5. Report
        if (error != null) {
            System.out.println("ERROR");
            System.err.println(error.getMessage());   // debug detail, not part of the spec
            return;
        }
        System.out.println("NO ERROR(S) FOUND");

        // 6. Execute
        /**
         * remove comment when Interpreter is done
         * new Interpreter().run(program);
         */
    }
}