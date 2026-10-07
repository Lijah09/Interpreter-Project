import java.util.ArrayList;
import java.util.List;

public class Parser {
	private final List<Token> tokens;
	private int current = 0;
	
	public Parser(List<Token> tokens) {
		this.tokens = tokens;
	}
	
	public List<Statement> parse() {
		List<Statement> statements = new ArrayList<>();
		
		while (!isAtEnd()) {
			statements.add(statement());
		}
		
		return statements;
	}
	
	private Statement statement() {
		if (check(Token.Type.IDENT) && checkNext(":")) {
			return declaration();
		}
		
		if (check(Token.Type.IDENT) && checkNext(":=")) {
			return assignment();
		}
		
		if (checkReserved("output")) {
			return output();
		}
		
		if (checkReserved("if")) {
			return ifStmt();
		}
		
		throw error(peek(), "expected statement");
	}
	
	//DECLARATION
	private Statement declaration() {
		Token name = consume(Token.Type.IDENT, "expected identifier");
		consumeSymbol(":", "expected ':' identifier");
		
		String type = type();
		
		consumeSymbol(";", "expected ';' after declaration");
		
		return new Declaration(name.lexeme, type, name.line);
	}
	
	//TYPE
	private String type() {
		if (checkReserved("integer")) {
			advance();
			return "integer";
		}
		
		if (checkReserved("double")) {
			advance();
			return "double";
		}
		
		throw error(peek(), "expected 'integer' or 'double'");
	}
	
	//ASSIGNMENT
	private Statement assignment() {
		Token name = consume(Token.Type.IDENT, "expected identifier");
		consumeSymbol(":=", "expected ':=' after identifier");
		
		Expression expression = expression();
		
		consumeSymbol(";", "expected ';' after assignment");
		
		return new Assignment(name.lexeme, expression, name.line);
	}
	
	//OUTPUT
	private  Statement output() {
		Token keyword = consumeReserved("output");
		
		consumeSymbol("<<", "expected '<<' after 'output'");
		
		if (check(Token.Type.STRING)) {
			Token string = advance();
			consumeSymbol(";", "expected ';' after output");
			
			return new Output(string.lexeme, true, keyword.line);
		}
		
		Expression expression = expression();
		consumeSymbol(";", "expected ';' after output");
		
		return new Output(expression, keyword.line);
	}
	
	//IF
	private Statement ifStmt() {
		Token keyword = consumeReserved("if");
		
		consumeSymbol("(", "expected '(' after 'if'");
		
		Expression left = expression();
		String operator = relop();
		Expression right = expression();
		
		consumeSymbol(")", "expected ')' after condition");
		
		Statement body = statement();
		
		return new IfStatement(left, operator, right, body, keyword.line);
	}
	
	//RELATIONAL OPERATORS
	private String relop() {
		if (checkSymbol("<") || checkSymbol(">") || checkSymbol("==") || checkSymbol("!=")) {
			return advance().lexeme;
		}
		
		throw error(peek(), "expected relational operator");
	}
	
	//EXPRESSION
	private Expression expression() {
		List<Operand> operands = new ArrayList<>();
		List<String> operators = new ArrayList<>();
		
		operands.add(operand());
		
		while (checkSymbol("+") || checkSymbol("-")) {
			operators.add(advance().lexeme);
			operands.add(operand());
		}
		
		return new Expression(operands, operators);
	}
	
	//OPERAND
	private Operand operand() {
		if (check(Token.Type.INT)) {
			Token token = advance();
			return new Operand(Operand.Type.INT, token.lexeme);
		}
		
		if (check(Token.Type.DOUBLE)) {
			Token token = advance();
			return new Operand(Operand.Type.DOUBLE, token.lexeme);
		}
		
		if (check(Token.Type.IDENT)) {
			Token token = advance();
			return new Operand(Operand.Type.IDENT, token.lexeme);
		}
		
		throw error(peek(), "expected integer, double, or identifier");
	}
	
	//TOKEN HELPERS
	private boolean isAtEnd() {
		return peek().type == Token.Type.EOF;
	}
	
	private Token peek() {
		return tokens.get(current);
	}
	
	private Token previous() {
		return tokens.get(current - 1);
	}
	
	private Token advance() {
		if(!isAtEnd()) {
			current++;
		}
		
		return previous();
	}
	
	private boolean check(Token.Type type) {
		return peek().type == type;
	}
	
	private boolean checkSymbol(String symbol) {
		return check(Token.Type.SYMBOL) && peek().lexeme.equals(symbol);
	}
	
	private boolean checkReserved(String word) {
		return check(Token.Type.RESERVED) && peek().lexeme.equalsIgnoreCase(word);
	}
	
	private boolean checkNext(String symbol) {
		if (current + 1 >= tokens.size()) {
			return false;
		}
		
		Token next = tokens.get(current + 1);
		
		return next.type == Token.Type.SYMBOL && next.lexeme.equals(symbol);
	}

    private Token consume(Token.Type type, String message) {

        if (check(type)) {
            return advance();
        }

        throw error(peek(), message);
    }
	
    private Token consumeSymbol(
            String symbol,
            String message
    ) {

        if (checkSymbol(symbol)) {
            return advance();
        }

        throw error(peek(), message);
    }
	
    private Token consumeReserved(
            String word
    ) {

        if (checkReserved(word)) {
            return advance();
        }

        throw error(
                peek(),
                "expected '" + word + "'"
        );
    }

    private HLError error(
            Token token,
            String message
    ) {

        return new HLError(message, token.line);
    }
	
    public abstract static class Statement {
        public final int line;

        protected Statement(int line) {
            this.line = line;
        }
    }

    public static class Declaration extends Statement {

        public final String name;
        public final String type;

        public Declaration(
                String name,
                String type,
                int line
        ) {
            super(line);
            this.name = name;
            this.type = type;
        }
    }

    public static class Assignment extends Statement {

        public final String name;
        public final Expression expression;

        public Assignment(
                String name,
                Expression expression,
                int line
        ) {
            super(line);
            this.name = name;
            this.expression = expression;
        }
    }

    public static class Output extends Statement {

        public final boolean isString;
        public final String stringValue;
        public final Expression expression;

        public Output(
                String stringValue,
                boolean isString,
                int line
        ) {
            super(line);
            this.stringValue = stringValue;
            this.isString = isString;
            this.expression = null;
        }

        public Output(
                Expression expression,
                int line
        ) {
            super(line);
            this.expression = expression;
            this.stringValue = null;
            this.isString = false;
        }
    }

    public static class IfStatement extends Statement {

        public final Expression left;
        public final String operator;
        public final Expression right;
        public final Statement body;

        public IfStatement(
                Expression left,
                String operator,
                Expression right,
                Statement body,
                int line
        ) {
            super(line);
            this.left = left;
            this.operator = operator;
            this.right = right;
            this.body = body;
        }
    }
	
    public static class Expression {

        public final List<Operand> operands;
        public final List<String> operators;

        public Expression(
                List<Operand> operands,
                List<String> operators
        ) {
            this.operands = operands;
            this.operators = operators;
        }
    }

	//EXPRESSION CLASSES	
    public static class Operand {

        public enum Type {
            INT,
            DOUBLE,
            IDENT
        }

        public final Type type;
        public final String value;

        public Operand(
                Type type,
                String value
        ) {
            this.type = type;
            this.value = value;
        }
    }
}