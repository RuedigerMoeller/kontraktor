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
    private QStack stackOperations = new QStack();
    /* stack for holding expression converted to reversed polish notation */
    private QStack stackRPN = new QStack();
    /* stack for holding the lambda calculation tree */
    private QStack stackAnswer = new QStack();

    protected EvalContext ctxRef[];


    Parser(HashMap<String, FuncOperand> functions, HashMap<String, Operator> operators) {
        this.functions = functions;
        this.operators = operators;
    }

    public CompiledQuery compile(String query) {
        ctxRef = new EvalContext[1];
        parse(query);
        return new CompiledQuery(new Evaluator(stackRPN).evaluate(),ctxRef);
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
                if ( stackOperations.empty() || !isOpenEckig(stackOperations.lastElement().toString()) ) {
                    // do this only in arglist, not array constant
                    while (!stackOperations.empty()
                        && !isOpenBracket(stackOperations.lastElement().toString())) {
                        stackRPN.push(stackOperations.pop());
                    }
                }
            } else if (isOpenEckig(tokenValue)) {
                stackRPN.push(tokenValue);
                stackOperations.push(tokenValue);
            } else if (isCloseEckig(tokenValue)) {
                while (!stackOperations.empty()
                    && !isOpenEckig(stackOperations.lastElement().toString())) {
                    stackRPN.push(stackOperations.pop());
                }
                List arr = new ArrayList();
                while ( !stackRPN.empty()
                    && !isOpenEckig(stackRPN.lastElement().toString())) {
                    arr.add(stackRPN.pop());
                }
                stackRPN.pop();
                stackOperations.pop(); // should be '['
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
                       isOpenEckig(prevToken.getValue()) ||
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
//
        MapRecord hm = MapRecord.New("key")
            .put("test","hallo")
            .put("a", 100)
            .put("c", -1)
            .put("time", System.currentTimeMillis())
            .put("b", 200);

        testArray(p, hm);

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

    protected static void testArray(Parser p, MapRecord hm) {
        CompiledQuery compile;
        Value evaluate;

        compile = p.compile("'hallo' ** [1,2,'ha'+'llo']");
        evaluate = compile.evaluate(hm);
        if ( ! evaluate.isTrue() )
            throw new RuntimeException("test failure");

        compile = p.compile("a ** [1,2,50+50]");
        evaluate = compile.evaluate(hm);
        if ( ! evaluate.isTrue() )
            throw new RuntimeException("test failure");

        compile = p.compile("a ** [1,2,100]");
        evaluate = compile.evaluate(hm);
        if ( ! evaluate.isTrue() )
            throw new RuntimeException("test failure");

        compile = p.compile("a ** [1,2,a]");
        evaluate = compile.evaluate(hm);
        if ( ! evaluate.isTrue() )
            throw new RuntimeException("test failure");

        compile = p.compile("100 ** [1,2,a]");
        evaluate = compile.evaluate(hm);
        if ( ! evaluate.isTrue() )
            throw new RuntimeException("test failure");

        compile = p.compile("'hallo' ** [1,2,test]");
        evaluate = compile.evaluate(hm);
        if ( ! evaluate.isTrue() )
            throw new RuntimeException("test failure");


        System.out.println("testArray success");
    }
}
