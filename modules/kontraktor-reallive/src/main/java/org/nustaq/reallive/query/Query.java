package org.nustaq.reallive.query;

import java.util.HashMap;

/**
 * Created by ruedi on 29/08/15.
 */
public class Query {

    public static synchronized CompiledQuery compile(String query) {
        return newParser().compile(query);
    }

    public static Value eval(String query,EvalContext ctx) {
        return newParser().compile(query).evaluate(ctx);
    }

    protected static Parser newParser() {

        HashMap<String,FuncOperand> functions = new HashMap();
        defaultFun(functions);

        HashMap<String,Operator> operators = new HashMap();
        defaultOps(operators);

        return new Parser(functions,operators);
    }

    protected static void defaultOps(HashMap<String, Operator> operators) {
        operators.put("!", new Operator("!",15,1) {
            @Override
            protected Value compare(Value vb, Value va) {
                return new LongValue(vb.isTrue()?0:1, null);
            }
        });
        operators.put("+", new Operator("+",7) {

            @Override
            protected Value compare(Value vb, Value va) {
                if ( va == null ) // unary
                {
                    return vb.negate();
                }
                return super.compare(vb, va);
            }

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

        // indexOf.toLowerCase >= 0
        operators.put("**", new Operator("**",6) {
            @Override
            protected Value compare(Value vb, Value va) {
                if ( va.isArray() ) {
                    Object bval = vb.getValue();
                    Value bvalue = vb;
                    Value avalue = va;
                    return compareArray(bval, bvalue, avalue);
                }
                if ( vb.isArray() ) {
                    Object bval = va.getValue();
                    Value bvalue = va;
                    Value avalue = vb;
                    return compareArray(bval, bvalue, avalue);
                }
                return va.getStringValue().toLowerCase().indexOf(vb.getStringValue().toLowerCase()) >= 0 ? Value.TRUE:Value.FALSE;
            }

            private Value compareArray(Object bval, Value bvalue, Value avalue) {
                Object[] ov = (Object[]) avalue.getValue();
                for (int i = 0; i < ov.length; i++) {
                    Object o = ov[i];
                    if ( o instanceof RLSupplier)
                        o = ((RLSupplier) o).get();
                    if ( o instanceof Number && bvalue instanceof NumberValue) {
                        if ( ((Number)o).doubleValue() == bvalue.getDoubleValue() )
                            return Value.TRUE;
                    } else if ( o instanceof NumberValue && bvalue instanceof NumberValue) {
                        if ( ((Value)o).getDoubleValue() == bvalue.getDoubleValue() )
                            return Value.TRUE;
                    } else {
                        if ( o instanceof Value )
                            o = ((Value) o).getStringValue();
                        if (o != null && o.toString().equals(bval.toString()))
                            return Value.TRUE;
                    }
                }
                return Value.FALSE;
            }
        });

        // toLowerCase.endswith
        operators.put("*>", new Operator("*>",6) {
            @Override
            protected Value compare(Value vb, Value va) {
                return va.getStringValue().toLowerCase().endsWith(vb.getStringValue().toLowerCase()) ? Value.TRUE:Value.FALSE;
            }
        });

        // toLowerCase.startswith
        operators.put("<*", new Operator("<*",6) {
            @Override
            protected Value compare(Value vb, Value va) {
                return va.getStringValue().toLowerCase().startsWith(vb.getStringValue().toLowerCase()) ? Value.TRUE:Value.FALSE;
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

//        operators.put("!=", new Operator("!=",6) {
//            @Override
//            protected long longOp(long longValue, long longValue1) {
//                return longValue != longValue1 ? 1:0;
//            }
//
//            @Override
//            protected double doubleOp(double doubleValue, double doubleValue1) {
//                return doubleValue!=doubleValue1 ?1:0;
//            }
//
//            @Override
//            protected String stringOp(String stringValue, String stringValue1) {
//                return !stringValue.equals(stringValue1) ? "1" : "0";
//            }
//        });
        operators.put("&&", new Operator("&&",3) {
            @Override
            public RLSupplier<Value> getEval( RLSupplier<Value> arg, RLSupplier<Value> arg1 ) {
                return () -> {
                    Value va = arg1.get();
                    if ( !va.isTrue() )
                        return Value.FALSE;
                    Value vb = arg.get();
                    return compare(vb, va);
                };
            }
            @Override
            protected Value compare(Value vb, Value va) {
                return vb.isTrue() && va.isTrue() ? Value.TRUE : Value.FALSE;
            }
        });
        operators.put("||", new Operator("||",3) {
            @Override
            public RLSupplier<Value> getEval( RLSupplier<Value> arg, RLSupplier<Value> arg1 ) {
                return () -> {
                    Value va = arg1.get();
                    if ( va.isTrue() )
                        return Value.TRUE;
                    Value vb = arg.get();
                    return compare(vb, va);
                };
            }
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
                return stringValue.compareTo(stringValue1) < 0 ? "1" : "";
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
                return stringValue.compareTo(stringValue1) <= 0 ? "1" : "";
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
                return stringValue.compareTo(stringValue1) > 0 ? "1" : "";
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
                return stringValue.compareTo(stringValue1) >= 0 ? "1" : "";
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
                return stringValue.compareTo(stringValue1) != 0 ? "1" : "";
            }
        });
    }

    protected static void defaultFun(HashMap<String, FuncOperand> functions) {

        // Todo:
        // arrays: sum avg contains
        // object: match by example

        functions.put("lower", new FuncOperand("lower",1) {
            @Override
            protected Value apply(RLSupplier<Value>[] args) {
                return new StringValue(args[0].get().getStringValue().toLowerCase(),null);
            }
        });

        functions.put("upper", new FuncOperand("upper",1) {
            @Override
            protected Value apply(RLSupplier<Value>[] args) {
                return new StringValue(args[0].get().getStringValue().toUpperCase(), null);
            }
        });

        functions.put("exists", new FuncOperand("exists",1) {
            @Override
            protected Value apply(RLSupplier<Value>[] args) {
                Value value = args[0].get();
                return value.isEmpty()? Value.FALSE:Value.TRUE;
            }
        });

        functions.put("isEmpty", new FuncOperand("isEmpty",1) {
            @Override
            protected Value apply(RLSupplier<Value>[] args) {
                Value value = args[0].get();
                return value.isEmpty()? Value.TRUE:Value.FALSE;
            }
        });

        functions.put("length", new FuncOperand("length",1) {
            @Override
            protected Value apply(RLSupplier<Value>[] args) {
                Value value = args[0].get();
                if ( value.isArray() )
                    return new LongValue( ((ArrayValue)value).size(), null );
                return new LongValue(0,null);
            }
        });
        // time ranges from now
        // time is settled at compile time
        functions.put("age", new FuncOperand("age",2) {

            @Override
            public RLSupplier<Value> getEval(RLSupplier<Value>[] args) {
                if ( args.length != arity ) {
                    String err = args.length > 0 ? args[args.length - 1].get().getErrorString() : " - ";
                    throw new QParseException("invalid number of arguments:" + name+ " "+err);
                }
                return new RLSupplier<Value>() {
                    long now = System.currentTimeMillis();
                    @Override
                    public Value get() {
                        return apply(args, now);
                    }
                };
            }

            final long minute = 1000L * 60L;
            final long hour = minute * 60L;
            final long day = hour * 24L;
            final long week = day * 7L;
            final long month = day * 30L;
            final long year = day * 365L;

            protected Value apply(RLSupplier<Value>[] args,long now) {
                long val = args[0].get().getLongValue();
                String sv = args[1].get().getStringValue();
                switch (sv) {
                    case "ms": val = now - val*1l; break;
                    case "sec": val = now - val*1000l; break;
                    case "min": val = now - val*minute; break;
                    case "hour": val = now - val*hour; break;
                    case "day": val = now - val*day; break;
                    case "week": val = now - val*week; break;
                    case "month": val = now - val*month; break;
                    case "year": val = now - val*year; break;
                    default: throw new QParseException("invalid arg in age(..):"+sv+", "+args[1].get().getErrorString());
                }
                return new LongValue(val,args[0].get().getToken());
            }
        });

        functions.put("now", new FuncOperand("now",0) {
            @Override
            public RLSupplier<Value> getEval(RLSupplier<Value>[] args) {
                if ( args.length != arity ) {
                    String err = args.length > 0 ? args[args.length - 1].get().getErrorString() : " - ";
                    throw new QParseException("invalid number of arguments:" + name+", "+err);
                }
                return new RLSupplier<Value>() {
                    long now = System.currentTimeMillis();
                    @Override
                    public Value get() {
                        return apply(args, now);
                    }
                };
            }

            protected Value apply(RLSupplier<Value>[] args,long now) {
                return new LongValue(now,null);
            }
        });

    }

}
