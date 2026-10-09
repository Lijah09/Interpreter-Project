import java.util.*;

/**
 * Semantic checker. Runs AFTER the parser and BEFORE the interpreter, so every
 * meaning-related error is found before the program prints anything.
 *
 * Rules checked:
 *   1. A variable can only be declared once.
 *   2. A variable must be declared before it is used or assigned.
 *   3. A double value cannot be assigned to an integer variable.
 *   4. A variable must have a value before it is used in an expression.
 *   5. A declaration cannot be the body of an if.
 *   6. Integer literals must fit in a Java long.
 * Throws HLError on the first problem found.
 */
public class Checker {
    private final Map<String, Boolean> declared = new HashMap<>();  // name -> true if double
    private final Set<String> assigned = new HashSet<>();           // names that definitely have a value

    public void check(List<Parser.Statement> program) {
        for (Parser.Statement s : program) {
            checkStatement(s, false);
        }
    }

    // 'conditional' is true inside an if-body. An assignment there might not run,
    // so it must not count as "this variable now has a value".
    private void checkStatement(Parser.Statement s, boolean conditional) {
        if (s instanceof Parser.Declaration) {
            Parser.Declaration d = (Parser.Declaration) s;
            if (conditional)
                throw new HLError("a declaration is not allowed inside an if", d.line);
            if (declared.containsKey(d.name))
                throw new HLError("variable '" + d.name + "' is already declared", d.line);
            declared.put(d.name, d.type.equals("double"));

        } else if (s instanceof Parser.Assignment) {
            Parser.Assignment a = (Parser.Assignment) s;
            if (!declared.containsKey(a.name))
                throw new HLError("variable '" + a.name + "' is not declared", a.line);
            boolean exprIsDouble = checkExpression(a.expression, a.line);
            if (exprIsDouble && !declared.get(a.name))
                throw new HLError("cannot assign a double value to integer variable '" + a.name + "'", a.line);
            if (!conditional) assigned.add(a.name);

        } else if (s instanceof Parser.Output) {
            Parser.Output o = (Parser.Output) s;
            if (!o.isString) checkExpression(o.expression, o.line);

        } else if (s instanceof Parser.IfStatement) {
            Parser.IfStatement i = (Parser.IfStatement) s;
            checkExpression(i.left, i.line);
            checkExpression(i.right, i.line);
            checkStatement(i.body, true);
        }
    }

    /** Checks every operand and returns true if the expression's type is double. */
    private boolean checkExpression(Parser.Expression e, int line) {
        boolean isDouble = false;
        for (Parser.Operand op : e.operands) {
            switch (op.type) {
                case INT:
                    try {
                        Long.parseLong(op.value);
                    } catch (NumberFormatException ex) {
                        throw new HLError("integer '" + op.value + "' is too large", line);
                    }
                    break;
                case DOUBLE:
                    isDouble = true;
                    break;
                case IDENT:
                    if (!declared.containsKey(op.value))
                        throw new HLError("variable '" + op.value + "' is not declared", line);
                    if (!assigned.contains(op.value))
                        throw new HLError("variable '" + op.value + "' has no value yet", line);
                    if (declared.get(op.value)) isDouble = true;
                    break;
            }
        }
        return isDouble;
    }
}