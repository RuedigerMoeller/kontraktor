package org.nustaq.reallive.query;

import java.text.ParseException;
import java.util.HashMap;

/**
 * Created by ruedi on 29/08/15.
 */
public class Query {

    public static CompiledQuery compile(String query) throws ParseException {
        return newParser().compile(query);
    }

    protected static Parser newParser() {

        HashMap<String,FuncOperand> functions = new HashMap<>();
        defaultFun(functions);

        HashMap<String,Operator> operators = new HashMap<>();
        defaultOps(operators);

        return new Parser(functions,operators);
    }

    protected static void defaultOps(HashMap<String, Operator> operators) {
        operators.put("!", new Operator("!",15,1) {
            @Override
            protected Value compare(Value vb, Value va) {
                return new LongValue(vb.isTrue()?0:1);
            }
        });
        operators.put("+", new Operator("+",7) {
            @Override
            protected long longOp(long longValue, long longValue1) {
                return longValue+longValue1;
            }

            @Override
            protected double doubleOp(double doubleValue, double doubleValue1) {
                return doubleValue+doubleValue1;
            }

            @Override
            protected String stringOp(String stringValue, String stringValue1) {
                return stringValue + stringValue1;
            }
        });
        operators.put("-", new Operator("-",7) {
            @Override
            protected long longOp(long longValue, long longValue1) {
                return longValue-longValue1;
            }

            @Override
            protected double doubleOp(double doubleValue, double doubleValue1) {
                return doubleValue-doubleValue1;
            }

            @Override
            protected String stringOp(String stringValue, String stringValue1) {
                if ( stringValue.endsWith(stringValue1) )
                    return stringValue.substring(0,stringValue.lastIndexOf(stringValue1));
                return stringValue;
            }
        });
        operators.put("*", new Operator("*") {
            @Override
            protected long longOp(long longValue, long longValue1) {
                return longValue*longValue1;
            }

            @Override
            protected double doubleOp(double doubleValue, double doubleValue1) {
                return doubleValue*doubleValue1;
            }

            @Override
            protected String stringOp(String stringValue, String stringValue1) {
                return stringValue+"*"+stringValue;
            }
        });
        operators.put("/", new Operator("/") {
            @Override
            protected long longOp(long longValue, long longValue1) {
                return longValue/longValue1;
            }

            @Override
            protected double doubleOp(double doubleValue, double doubleValue1) {
                return doubleValue/doubleValue1;
            }

            @Override
            protected String stringOp(String stringValue, String stringValue1) {
                return stringValue.replace(stringValue1,"");
            }
        });

        operators.put("==", new Operator("==",6) {
            @Override
            protected long longOp(long longValue, long longValue1) {
                return longValue == longValue1 ? 1:0;
            }

            @Override
            protected double doubleOp(double doubleValue, double doubleValue1) {
                return doubleValue==doubleValue1 ?1:0;
            }

            @Override
            protected String stringOp(String stringValue, String stringValue1) {
                return stringValue.equals(stringValue1) ? "1" : "0";
            }
        });
        operators.put("!=", new Operator("!=",6) {
            @Override
            protected long longOp(long longValue, long longValue1) {
                return longValue != longValue1 ? 1:0;
            }

            @Override
            protected double doubleOp(double doubleValue, double doubleValue1) {
                return doubleValue!=doubleValue1 ?1:0;
            }

            @Override
            protected String stringOp(String stringValue, String stringValue1) {
                return !stringValue.equals(stringValue1) ? "1" : "0";
            }
        });
        operators.put("&&", new Operator("&&",3) {
            @Override
            protected Value compare(Value vb, Value va) {
                return vb.isTrue() && va.isTrue() ? Value.TRUE : Value.FALSE;
            }
        });
        operators.put("||", new Operator("||",3) {
            @Override
            protected Value compare(Value vb, Value va) {
                return vb.isTrue() || va.isTrue() ? Value.TRUE : Value.FALSE;
            }
        });
        operators.put("^", new Operator("^",3) {
            @Override
            protected Value compare(Value vb, Value va) {
                return vb.isTrue() ^ va.isTrue() ? Value.TRUE : Value.FALSE;
            }
        });
        operators.put("<", new Operator("<",6) {
            @Override
            protected long longOp(long longValue, long longValue1) {
                return longValue < longValue1 ? 1:0;
            }

            @Override
            protected double doubleOp(double doubleValue, double doubleValue1) {
                return doubleValue<doubleValue1 ? 1 : 0;
            }

            @Override
            protected String stringOp(String stringValue, String stringValue1) {
                return stringValue.compareTo(stringValue) < 0 ? "1" : "";
            }
        });
        operators.put("<=", new Operator("<=",6) {
            @Override
            protected long longOp(long longValue, long longValue1) {
                return longValue <= longValue1 ? 1:0;
            }

            @Override
            protected double doubleOp(double doubleValue, double doubleValue1) {
                return doubleValue<=doubleValue1 ? 1 : 0;
            }

            @Override
            protected String stringOp(String stringValue, String stringValue1) {
                return stringValue.compareTo(stringValue) <= 0 ? "1" : "";
            }
        });
        operators.put(">", new Operator(">",6) {
            @Override
            protected long longOp(long longValue, long longValue1) {
                return longValue > longValue1 ? 1:0;
            }

            @Override
            protected double doubleOp(double doubleValue, double doubleValue1) {
                return doubleValue>doubleValue1 ? 1 : 0;
            }

            @Override
            protected String stringOp(String stringValue, String stringValue1) {
                return stringValue.compareTo(stringValue) > 0 ? "1" : "";
            }
        });
        operators.put(">=", new Operator(">=",6) {
            @Override
            protected long longOp(long longValue, long longValue1) {
                return longValue >= longValue1 ? 1:0;
            }

            @Override
            protected double doubleOp(double doubleValue, double doubleValue1) {
                return doubleValue>=doubleValue1 ? 1 : 0;
            }

            @Override
            protected String stringOp(String stringValue, String stringValue1) {
                return stringValue.compareTo(stringValue) >= 0 ? "1" : "";
            }
        });
        operators.put("!=", new Operator("!=",6) {
            @Override
            protected long longOp(long longValue, long longValue1) {
                return longValue != longValue1 ? 1:0;
            }

            @Override
            protected double doubleOp(double doubleValue, double doubleValue1) {
                return doubleValue!=doubleValue1 ? 1 : 0;
            }

            @Override
            protected String stringOp(String stringValue, String stringValue1) {
                return stringValue.compareTo(stringValue) != 0 ? "1" : "";
            }
        });
    }

    protected static void defaultFun(HashMap<String, FuncOperand> functions) {
        functions.put("startsWith", new FuncOperand("startsWith"));
        functions.put("endsWith", new FuncOperand("endsWith"));
        functions.put("contains", new FuncOperand("contains"));
    }

}
