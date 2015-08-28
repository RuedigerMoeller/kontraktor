/**
 * derived from bracer parser
 */

/*
 * Copyright 2014 Dmytro Titov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nustaq.reallive.query;

import java.text.ParseException;
import java.util.*;
import java.util.function.Supplier;

/**
 * Class for parsing and evaluating math expressions
 *
 * @author Dmytro Titov
 * @version 7.0
 * @since 1.0
 *
 * modified + extended by r.moeller
 */
public class Parser {

    /* list of available functions */
    HashMap<String,FuncOperand> functions;
    {
        functions = new HashMap<>();
        functions.put("startsWith", new FuncOperand("startsWith"));
        functions.put("endsWith", new FuncOperand("endsWith"));
        functions.put("contains", new FuncOperand("contains"));
    }

    /* list of available operators */
    HashMap<String,Operator> operators;
    {
        operators = new HashMap<>();
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
//        operators.put("|", new Operator("|",3));
//        operators.put("!", new Operator("!"));
//        operators.put("<", new Operator("<",6));
//        operators.put(">", new Operator(">",6));
    }
    /* separator of arguments */
    private final String SEPARATOR = ",";
    /* temporary stack that holds operators, functions and brackets */
    private Stack stackOperations = new Stack();
    /* stack for holding expression converted to reversed polish notation */
    private Stack stackRPN = new Stack();
    /* stack for holding the lambda calculation tree */
    private Stack stackAnswer = new Stack();


    /**
     * Parses the math expression (complicated formula) and stores the result
     *
     * @param expression <code>String</code> input expression (math formula)
     * @throws <code>ParseException</code> if the input expression is not
     *                                     correct
     * @since 3.0
     */
    public void parse(String expression) throws ParseException {
        /* cleaning stacks */
        stackOperations.clear();
        stackRPN.clear();

		/*
		 * make some preparations: remove spaces; handle unary + and -, handle
		 * degree character
		 */
//        expression = expression.replace(" ", "")
//                .replace("°", "*" + Double.toString(Math.PI) + "/180")
//                .replace("(-", "(0-").replace(",-", ",0-").replace("(+", "(0+")
//                .replace(",+", ",0+");
//        if (expression.charAt(0) == '-' || expression.charAt(0) == '+') {
//            expression = "0" + expression;
//        }
        QScanner scanner = new QScanner(expression);
		/* loop for handling each token - shunting-yard algorithm */
        String stringToken;
        while ( (stringToken=scanner.readNext()) != null) {
            if (isSeparator(stringToken)) {
                while (!stackOperations.empty()
                        && !isOpenBracket(stackOperations.lastElement())) {
                    stackRPN.push(stackOperations.pop());
                }
            } else if (isOpenBracket(stringToken)) {
                stackOperations.push(stringToken);
            } else if (isCloseBracket(stringToken)) {
                while (!stackOperations.empty()
                        && !isOpenBracket(stackOperations.lastElement())) {
                    stackRPN.push(stackOperations.pop());
                }
                stackOperations.pop();
                if (!stackOperations.empty()
                        && stackOperations.lastElement() instanceof FuncOperand) {
                    stackRPN.push(stackOperations.pop());
                }
            } else if (isNumber(stringToken)) {
                if ( stringToken.indexOf('.')<0) {
                    Long i = Long.parseLong(stringToken);
                    stackRPN.push( new LongValue(i) );
                } else {
                    Double d = Double.parseDouble(stringToken);
                    stackRPN.push( new DoubleValue(d) );
                }
            } else if (operators.containsKey(stringToken)) {
                Operator op = operators.get(stringToken);
                while (!stackOperations.empty()
                        && stackOperations.lastElement() instanceof Operator
                        && op.getPrecedence() <= ((Operator)stackOperations.lastElement()).getPrecedence() ) {
                    stackRPN.push(stackOperations.pop());
                }
                stackOperations.push(op);
            } else if (isFunction(stringToken)) {
                stackOperations.push( functions.get(stringToken) );
            } else {
                if ( stringToken.startsWith("'") && stringToken.endsWith("'") ) {
                    stackRPN.push(new StringValue(stringToken.substring(1, stringToken.length() - 1)));
                } else //if ( stringToken.startsWith(VARIABLE+".") )
                {
                    stackRPN.push(new VarPath(stringToken));
                }
//                else throw new ParseException("Unrecognized stringToken: " + stringToken, 0);
            }
        }
        while (!stackOperations.empty()) {
            stackRPN.push(stackOperations.pop());
        }

		/* reverse stack */
        Collections.reverse(stackRPN);
    }

    /**
     * Evaluates once parsed to a lambda term
     *
     * @return <code>String</code> representation of the result
     * @throws <code>ParseException</code> if the input expression is not
     *                                     correct
     * @since 3.0
     */
    public Supplier<Value> evaluate() throws ParseException {
		/* check if is there something to evaluate */
        if (stackRPN.empty()) {
            return () -> new StringValue("");
        }

		/* clean answer stack */
        stackAnswer.clear();

		/* get the clone of the RPN stack for further evaluating */
        @SuppressWarnings("unchecked")
        Stack stackRPN = (Stack) this.stackRPN.clone();

		/* enroll the variable value into expression */
//        Collections.replaceAll(stackRPN, VARIABLE,
//                Double.toString(variableValue));

		/* evaluating the RPN expression */
        while (!stackRPN.empty()) {
            Object token = stackRPN.pop();
            if (token instanceof Value) {
                stackAnswer.push( (Supplier) ()->token );
            } else if (token instanceof Operator) {
                Supplier a = (Supplier) stackAnswer.pop();
                Supplier b = (Supplier) stackAnswer.pop();
                stackAnswer.push( ((Operator) token).getEval(a,b));
//                switch (token)
                {
//                    case "+":
//                        stackAnswer.push(complexFormat.format(b.add(a)));
//                        break;
//                    case "-":
//                        stackAnswer.push(complexFormat.format(b.subtract(a)));
//                        break;
//                    case "*":
//                        stackAnswer.push(complexFormat.format(b.multiply(a)));
//                        break;
//                    case "/":
//                        stackAnswer.push(complexFormat.format(b.divide(a)));
//                        break;
//                    case "|":
//                        stackAnswer.push(String.valueOf(aBoolean || bBoolean ? "1" : "0"));
//                        break;
//                    case "&":
//                        stackAnswer.push(String.valueOf(aBoolean && bBoolean ? "1" : "0"));
//                        break;
                }
            } else if (token instanceof FuncOperand) {
//                switch (token)
                {
//                    case "abs":
//                        stackAnswer.push(complexFormat.format(a.abs()));
//                        break;
//                    case "pow":
//                        Complex b = complexFormat.parse(stackAnswer.pop());
//                        stackAnswer.push(complexFormat.format(b.pow(a)));
//                        break;
//                    case "not":
//                        stackAnswer.push(String.valueOf(!aBoolean ? "1" : "0"));
//                        break;
                }
            }
        }

        if (stackAnswer.size() > 1) {
            throw new ParseException("Some operator is missing", 0);
        }

        return (Supplier<Value>) stackAnswer.pop();
    }

    /**
     * Get back an <b>unmodifiable copy</b> of the stack
     */
    public Collection<String> getStackRPN() {
        return Collections.unmodifiableCollection(stackRPN);
    }

    private boolean isNumber(String token) {
        try {
            Double.parseDouble(token);
            return true;
        } catch (Exception e) {
        }
        return false;
    }
    private boolean isFunction(String token) {
        return functions.containsKey(token);
    }
    private boolean isSeparator(String token) {
        return token.equals(SEPARATOR);
    }
    private boolean isOpenBracket(Object token) {
        return "(".equals(token);
    }
    private boolean isCloseBracket(String token) {
        return token.equals(")");
    }

    public static void main(String[] args) throws ParseException {
        Parser p = new Parser();
//        p.parse("it.key+'3' < 10 | (it.test-4) = 3 & contains(it.name)");
//        p.parse("(2*4 == 8) && (3*3 == 9)");
        p.parse("2*4 + 13.44 / 2 -1");
        Supplier evaluate = (Supplier) p.evaluate();
        System.out.println(evaluate.get());
//        System.out.println(2*4 + 13.44 / 2 -1);
    }
}
