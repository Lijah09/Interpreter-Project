import java.math.BigDecimal;
import java.util.*;

public class Interpreter {
    
    private static final class Value {
        final long intValue;
        final BigDecimal dec;

        private Value(long intValue, BigDecimal dec) {
            this.intValue = intValue;
            this.dec = dec;
        }

        static Value ofInt(long v) {
            return new Value(v, null);
        }
        
        static Value ofDouble(BigDecimal v) {
            return new Value(0, v);
        }

        boolean isDouble() {
            return dec != null;
        }

        BigDecimal toDecimal() {
            return isDouble() ? dec : BigDecimal.valueOf(intValue);
        }
    }

    private final Map<String, Boolean> isDoubleVar = new HashMap<>();
    private final Map<String, Value> memory = new HashMap<>();

    public void run(List<Parser.Statement> program) {
        for (Parser.Statement s : program) {
            execute(s);
        }
    }

    private void execute(Parser.Statement s) {
        if (s instanceof Parser.Declaration) {
            Parser.Declaration d = (Parser.Declaration) s;
            isDoubleVar.put(d.name, d.type.equals("double"));
 
        } else if (s instanceof Parser.Assignment) {
            Parser.Assignment a = (Parser.Assignment) s;
            Value v = evaluate(a.expression, a.line);
            if (isDoubleVar.get(a.name)) {
                v = Value.ofDouble(v.toDecimal());     // integer value stored into a double variable
            }
            memory.put(a.name, v);
 
        } else if (s instanceof Parser.Output) {
            Parser.Output o = (Parser.Output) s;
            if (o.isString) {
                System.out.println(o.stringValue);
            } else {
                System.out.println(format(evaluate(o.expression, o.line)));
            }
 
        } else if (s instanceof Parser.IfStatement) {
            Parser.IfStatement i = (Parser.IfStatement) s;
            Value left = evaluate(i.left, i.line);
            Value right = evaluate(i.right, i.line);
            if (compare(left, right, i.operator)) {
                execute(i.body);
            }
        }
    }

    private Value evaluate(Parser.Expression e, int line) {
        Value result = operandValue(e.operands.get(0), line);
        for (int i = 0; i < e.operators.size(); i++) {
            Value next = operandValue(e.operands.get(i + 1), line);
            result = e.operators.get(i).equals("+") ? add(result, next, line)
                                                    : subtract(result, next, line);
        }
        return result;
    }
 
    private Value operandValue(Parser.Operand op, int line) {
        switch (op.type) {
            case INT:
                return Value.ofInt(Long.parseLong(op.value));     // Checker verified it fits
            case DOUBLE:
                return Value.ofDouble(new BigDecimal(op.value));
            default: {                                            // IDENT
                Value v = memory.get(op.value);
                if (v == null)                                    // should be impossible after the Checker
                    throw new HLError("variable '" + op.value + "' has no value", line);
                return v;
            }
        }
    }

    private Value add(Value a, Value b, int line) {
        if (a.isDouble() || b.isDouble())
            return Value.ofDouble(a.toDecimal().add(b.toDecimal()));
        try {
            return Value.ofInt(Math.addExact(a.intValue, b.intValue));
        } catch (ArithmeticException ex) {
            throw new HLError("integer overflow", line);
        }
    }
 
    private Value subtract(Value a, Value b, int line) {
        if (a.isDouble() || b.isDouble())
            return Value.ofDouble(a.toDecimal().subtract(b.toDecimal()));
        try {
            return Value.ofInt(Math.subtractExact(a.intValue, b.intValue));
        } catch (ArithmeticException ex) {
            throw new HLError("integer overflow", line);
        }
    }
 
    private boolean compare(Value a, Value b, String op) {
        int cmp;
        if (a.isDouble() || b.isDouble()) {
            cmp = a.toDecimal().compareTo(b.toDecimal());   // compareTo ignores scale: 1.5 == 1.50
        } else {
            cmp = Long.compare(a.intValue, b.intValue);
        }
        switch (op) {
            case "<":  return cmp < 0;
            case ">":  return cmp > 0;
            case "==": return cmp == 0;
            default:   return cmp != 0;                     // "!="
        }
    }

    private String format(Value v) {
        if (v.isDouble()) return v.dec.setScale(2).toPlainString();   // exact: values never exceed 2 decimals
        return Long.toString(v.intValue);
    }
}
