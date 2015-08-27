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
        operators.put("+", new Operator("+",7));
        operators.put("-", new Operator("-",7));
        operators.put("*", new Operator("*"));
        operators.put("/", new Operator("/"));
        operators.put("&", new Operator("&",3));
        operators.put("|", new Operator("|",3));
        operators.put("!", new Operator("!"));
        operators.put("=", new Operator("=",6));
        operators.put("<", new Operator("<",6));
        operators.put(">", new Operator(">",6));
    }
    /* separator of arguments */
    private final String SEPARATOR = ",";
    /* variable token */
    private final String VARIABLE = "it";
    /* temporary stack that holds operators, functions and brackets */
    private Stack stackOperations = new Stack();
    /* stack for holding expression converted to reversed polish notation */
    private Stack stackRPN = new Stack();
    /* stack for holding the calculations result */
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
        expression = expression.replace(" ", "")
                .replace("°", "*" + Double.toString(Math.PI) + "/180")
                .replace("(-", "(0-").replace(",-", ",0-").replace("(+", "(0+")
                .replace(",+", ",0+");
        if (expression.charAt(0) == '-' || expression.charAt(0) == '+') {
            expression = "0" + expression;
        }
		/* splitting input string into tokens */
        StringBuilder ops = new StringBuilder();
        operators.keySet().forEach(opString -> ops.append(opString));
        StringTokenizer stringTokenizer = new StringTokenizer(expression,
                ops.toString() + SEPARATOR + "()", true);

		/* loop for handling each token - shunting-yard algorithm */
        while (stringTokenizer.hasMoreTokens()) {
            String stringToken = stringTokenizer.nextToken();
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
                        && isFunction(stackOperations.lastElement())) {
                    stackRPN.push(stackOperations.pop());
                }
            } else if (isNumber(stringToken)) {
                if ( stringToken.indexOf('.')<0) {
                    Integer i = Integer.parseInt(stringToken);
                    stackRPN.push( new NumberOperand(i) );
                } else {
                    Double d = Double.parseDouble(stringToken);
                    stackRPN.push( new NumberOperand(d) );
                }
            } else if (operators.containsKey(stringToken)) {
                Operator op = operators.get(stringToken);
                while (!stackOperations.empty()
                        && operators.containsKey(stackOperations.lastElement())
                        && op.getPrecedence() <= ((Operator)stackOperations.lastElement()).getPrecedence() ) {
                    stackRPN.push(stackOperations.pop());
                }
                stackOperations.push(op);
            } else if (isFunction(stringToken)) {
                stackOperations.push( functions.get(stringToken) );
            } else {
                if ( stringToken.startsWith("'") && stringToken.endsWith("'") ) {
                    stackRPN.push(new StringConstant(stringToken));
                } else if ( stringToken.startsWith(VARIABLE+".") ) {
                    stackRPN.push(new VarPath(stringToken));
                } else
                    throw new ParseException("Unrecognized stringToken: " + stringToken, 0);
            }
        }
        while (!stackOperations.empty()) {
            stackRPN.push(stackOperations.pop());
        }

		/* reverse stack */
        Collections.reverse(stackRPN);
    }

    /**
     * Evaluates once parsed math expression with no variable included
     *
     * @return <code>String</code> representation of the result
     * @throws <code>ParseException</code> if the input expression is not
     *                                     correct
     * @since 1.0
     */
    public Object evaluate() throws ParseException {
        if (!stackRPN.contains("var")) {
            return evaluate(0);
        }
        throw new ParseException("Unrecognized token: var", 0);
    }

    /**
     * Evaluates once parsed math expression with "var" variable included
     *
     * @param variableValue User-specified <code>Double</code> value
     * @return <code>String</code> representation of the result
     * @throws <code>ParseException</code> if the input expression is not
     *                                     correct
     * @since 3.0
     */
    public Object evaluate(double variableValue) throws ParseException {
		/* check if is there something to evaluate */
        if (stackRPN.empty()) {
            return "";
        }

		/* clean answer stack */
        stackAnswer.clear();

		/* get the clone of the RPN stack for further evaluating */
        @SuppressWarnings("unchecked")
        Stack stackRPN = (Stack) this.stackRPN.clone();

		/* enroll the variable value into expression */
        Collections.replaceAll(stackRPN, VARIABLE,
                Double.toString(variableValue));

		/* evaluating the RPN expression */
        while (!stackRPN.empty()) {
            Token token = (Token) stackRPN.pop();
            if (token instanceof Operand) {
                stackAnswer.push(token);
            } else if (token instanceof Operator) {
                Operand a = (Operand) stackAnswer.pop();
                Operand b = (Operand) stackAnswer.pop();

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

        return stackAnswer.pop();
    }

    /**
     * Get back an <b>unmodifiable copy</b> of the stack
     */
    public Collection<String> getStackRPN() {
        return Collections.unmodifiableCollection(stackRPN);
    }

    /**
     * Check if the token is number (0-9, <code>IMAGINARY</code> or
     * <code>VARIABLE</code>)
     *
     * @param token Input <code>String</code> token
     * @return <code>boolean</code> output
     * @since 1.0
     */
    private boolean isNumber(String token) {
        try {
            Double.parseDouble(token);
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * Check if the token is function (e.g. "sin")
     *
     * @param token Input <code>String</code> token
     * @return <code>boolean</code> output
     * @since 1.0
     */
    private boolean isFunction(Object token) {
        return functions.containsKey(token);
    }

    /**
     * Check if the token is <code>SEPARATOR</code>
     *
     * @param token Input <code>String</code> token
     * @return <code>boolean</code> output
     * @since 1.0
     */
    private boolean isSeparator(String token) {
        return token.equals(SEPARATOR);
    }

    /**
     * Check if the token is opening bracket
     *
     * @param token Input <code>String</code> token
     * @return <code>boolean</code> output
     * @since 1.0
     */
    private boolean isOpenBracket(Object token) {
        return "(".equals(token);
    }

    /**
     * Check if the token is closing bracket
     *
     * @param token Input <code>String</code> token
     * @return <code>boolean</code> output
     * @since 1.0
     */
    private boolean isCloseBracket(String token) {
        return token.equals(")");
    }

    public static void main(String[] args) throws ParseException {
        Parser p = new Parser();
        p.parse("it.key+'3' < 10 | (it.test-4) = 3 & contains(it.name)");
        System.out.println(p.evaluate());
    }
}
