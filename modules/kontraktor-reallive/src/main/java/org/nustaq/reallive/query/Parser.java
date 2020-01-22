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

import org.nustaq.reallive.records.MapRecord;

import java.util.*;

/**
 * Class for parsing and evaluating query expressions
 *
 * @author Dmytro Titov
 * @version 7.0
 * @since 1.0
 *
 * modified + massively extended by r.moeller
 */
public class Parser {

    HashMap<String,FuncOperand> functions;
    HashMap<String,Operator> operators;

    /* separator of arguments */
    private final String SEPARATOR = ",";
    /* temporary stack that holds operators, functions and brackets */
    private Stack stackOperations = new Stack();
    /* stack for holding expression converted to reversed polish notation */
    private Stack stackRPN = new Stack();
    /* stack for holding the lambda calculation tree */
    private Stack stackAnswer = new Stack();

    protected EvalContext ctxRef[];


    Parser(HashMap<String, FuncOperand> functions, HashMap<String, Operator> operators) {
        this.functions = functions;
        this.operators = operators;
    }

    public CompiledQuery compile(String query) {
        ctxRef = new EvalContext[1];
        parse(query);
        return new CompiledQuery(evaluate(),ctxRef);
    }

    /**
     * Parses the math expression (complicated formula) and stores the result
     *
     * @param expression <code>String</code> input expression (math formula)
     * @throws <code>ParseException</code> if the input expression is not
     *                                     correct
     * @since 3.0
     */
    protected void parse(String expression) {
        /* cleaning stacks */
        stackOperations.clear();
        stackRPN.clear();

        QScanner scanner = new QScanner(expression);

		/* loop for handling each token - shunting-yard algorithm */
        QToken token,prevToken = null;
        while ( (token=scanner.readNext()) != null) {
            String tokenValue = token.getValue();
            if (isSeparator(tokenValue)) {
                while (!stackOperations.empty()
                        && !isOpenBracket(stackOperations.lastElement().toString())) {
                    stackRPN.push(stackOperations.pop());
                }
            } else if (isOpenEckig(tokenValue)) {
                stackOperations.push(tokenValue);
            } else if (isCloseEckig(tokenValue)) {
                List arr = new ArrayList();
                while ( !stackRPN.empty()
                    && !isOpenEckig(stackRPN.lastElement().toString())) {
                    arr.add(stackRPN.pop());
                }
                stackRPN.pop();
                stackRPN.push(new ArrayValue(arr.toArray(),token));
            } else if (isOpenBracket(tokenValue)) {
                Object last = stackRPN.isEmpty() ? null : stackRPN.lastElement();
                if ( last instanceof VarPath && isFunction(((VarPath) last).field)) {
                    stackRPN.pop();
                    stackOperations.push( functions.get(((VarPath) last).field) );
                }
                stackOperations.push(token);
            } else if (isCloseBracket(tokenValue)) {
                while (!stackOperations.empty()
                        && !isOpenBracket(stackOperations.lastElement().toString())) {
                    stackRPN.push(stackOperations.pop());
                }
                stackOperations.pop();
                if (!stackOperations.empty()
                        && stackOperations.lastElement() instanceof FuncOperand) {
                    stackRPN.push(stackOperations.pop());
                }
            } else if (isNumber(tokenValue)) {
                if ( tokenValue.indexOf('.')<0) {
                    Long i = Long.parseLong(tokenValue);
                    stackRPN.push( new LongValue(i,token) );
                } else {
                    Double d = Double.parseDouble(tokenValue);
                    stackRPN.push( new DoubleValue(d,token) );
                }
            } else if (operators.containsKey(tokenValue)) {
                Operator op = operators.get(tokenValue);
                // test for prefix op
                boolean prefix = false;
                if ( (tokenValue.equals("+") || tokenValue.equals("-")) &&
                         (prevToken == null ||
                       operators.containsKey(prevToken.getValue()) ||
                       isOpenBracket(prevToken.getValue()) ||
                       isSeparator(prevToken.getValue())
                     )
                   ) {
                    prefix = true;
                    stackRPN.push(new LongValue(0,token));
                }
                while (!stackOperations.empty() && ! prefix
                        && stackOperations.lastElement() instanceof Operator
                        && op.getPrecedence() <= ((Operator)stackOperations.lastElement()).getPrecedence() ) {
                    stackRPN.push(stackOperations.pop());
                }
                stackOperations.push(op);
            } else {
                if ( tokenValue.startsWith("'") && tokenValue.endsWith("'") ) {
                    stackRPN.push(new StringValue(tokenValue.substring(1, tokenValue.length() - 1),token));
                } else if ( tokenValue.startsWith("\"") && tokenValue.endsWith("\"") ) {
                    stackRPN.push(new StringValue(tokenValue.substring(1, tokenValue.length() - 1),token));
                } else
                {
                    if ( "true".equals(tokenValue) ) {
                        stackRPN.push(new BooleanValue(true,token));
                    } else if ( "false".equals(tokenValue) ) {
                        stackRPN.push(new BooleanValue(false,token));
                    } else
                        stackRPN.push(new VarPath(tokenValue,ctxRef,token));
                }
            }
            prevToken = token;
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
    private RLSupplier<Value> evaluate() {
		/* check if is there something to evaluate */
        if (stackRPN.empty()) {
            return () -> new StringValue("", null);
        }

		/* clean answer stack */
        stackAnswer.clear();

		/* get the clone of the RPN stack for further evaluating */
        @SuppressWarnings("unchecked")
        Stack stackRPN = (Stack) this.stackRPN.clone();

		/* evaluating the RPN expression */
        while (!stackRPN.empty()) {
            Object token = stackRPN.pop();
            if (token instanceof Value) {
                stackAnswer.push( (RLSupplier) ()->token );
            } else if (token instanceof Operator) {
                int arity = ((Operator) token).getArity();
                if ( arity == 2 ) {
                    RLSupplier a = (RLSupplier) stackAnswer.pop();
                    RLSupplier b = (RLSupplier) stackAnswer.pop();
                    stackAnswer.push( ((Operator) token).getEval(a,b));
                } else { // assume 1
                    RLSupplier a = (RLSupplier) stackAnswer.pop();
                    stackAnswer.push( ((Operator) token).getEval(a,null));
                }
            } else if (token instanceof VarPath) {
                VarPath vp = (VarPath) token;
                stackAnswer.push( vp.getEval() );
            } else if (token instanceof FuncOperand) {
                FuncOperand func = (FuncOperand) token;
                if ( func.getArity() < 0 ) {
                    int size = stackAnswer.size();
                    RLSupplier<Value> args[] = new RLSupplier[size];
                    for (int i = 0; i < size; i++) {
                        args[size - i - 1] = ((RLSupplier<Value>) stackAnswer.pop());
                    }
                } else {
                    RLSupplier<Value> args[] = new RLSupplier[func.getArity()];
                    for (int i = 0; i < args.length; i++) {
                        args[args.length - i - 1] = ((RLSupplier<Value>) stackAnswer.pop());
                    }
                    stackAnswer.push(func.getEval(args));
                }
            }
        }

        if (stackAnswer.size() > 1) {
            throw new QParseException("Missing or unknown operator:"+findNearesToken(stackAnswer));
        }

        return (RLSupplier<Value>) stackAnswer.pop();
    }

    private String findNearesToken(Stack stack) {
        int i = stack.size()-1;
        while ( i >= 0 ) {
            Object o = stack.get(i);
            if ( o instanceof HasToken) {
                return ((HasToken) o).getErrorString();
            }
            try {
                boolean b = o instanceof RLSupplier;
                if (b) {
                    Object val = ((RLSupplier) o).get();
                    if ( val instanceof HasToken) {
                        HasToken hasToken = (HasToken) val;
                        return hasToken != null ? hasToken.getErrorString() : "null";
                    }
                }
            } catch (Exception e) {
                //System.out.println("POK");
            }
            i--;
        }
        return null;
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
    private boolean isOpenBracket(String token) {
        return "(".equals(token);
    }
    private boolean isCloseBracket(String token) {
        return token.equals(")");
    }

    private boolean isOpenEckig(String token) {
        return "[".equals(token);
    }
    private boolean isCloseEckig(String token) {
        return token.equals("]");
    }
    public static void main(String[] args) throws Throwable {
        Parser p = Query.newParser();
        CompiledQuery compile = p.compile("1 ** [1,2]");
//
        MapRecord hm = MapRecord.New("key")
            .put("test","hallo")
            .put("a", 100)
            .put("c", -1)
            .put("time", System.currentTimeMillis())
            .put("b", 200);

        Thread.sleep(2000);
        CompiledQuery nums = Query.compile("c!= -1");
        System.out.println(nums.evaluate(hm));
//        if ( 1 != 0 )
//            return;
        CompiledQuery ctrue = Query.compile("time < age(1,'sec')");
        CompiledQuery cfalse = Query.compile("time < age(5,'sec')");
        CompiledQuery tim = Query.compile("a<1000000000");
        CompiledQuery trUe = Query.compile("1");
        System.out.println(ctrue.evaluate(hm));
        System.out.println(cfalse.evaluate(hm));
        System.out.println(tim.evaluate(hm));
        System.out.println(trUe.evaluate(hm).isTrue());

//
//        System.out.println(
//            compile.evaluate(hm)
//        );

//          System.out.println(Query.eval(" lower('HELLO'+'O')+'O' ",null));
//          System.out.println(Query.eval("lower('HELLO'+'O')", null));
//        System.out.println(Query.compile("3 * -3").evaluate(null));
//        System.out.println(Query.compile("3555 ** 55").evaluate(null));
//        System.out.println(Query.compile("'RuedI' ** 'dii'").evaluate(null));

//        while( true ) {
//            long now = System.currentTimeMillis();
//            int cnt = 0;
//            while( System.currentTimeMillis() - now < 1000 ) {
//                compile.evaluate(hm);
//                cnt++;
//            }
//            System.out.println("COUNT: "+cnt);
//        }

    }
}
