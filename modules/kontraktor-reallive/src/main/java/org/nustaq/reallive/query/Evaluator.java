package org.nustaq.reallive.query;

import java.io.Serializable;

/**
 * Evaluates once parsed to a lambda term
 *
 * @return <code>String</code> representation of the result
 * @throws <code>ParseException</code> if the input expression is not
 *                                     correct
 * @since 3.0
 */
public class Evaluator implements Serializable {

    private QStack stackRPN;
    /* stack for holding the lambda calculation tree */
    private QStack stackAnswer = new QStack();

    public Evaluator(QStack stackRPN) {
        this.stackRPN = stackRPN;
    }

    public RLSupplier<Value> evaluate() {
        return evaluate(false);
    }

    public RLSupplier<Value> evaluate(boolean valueArray) {
        /* check if is there something to evaluate */
        if (stackRPN.empty()) {
            return () -> new StringValue("", null);
        }

        /* clean answer stack */
        stackAnswer.clear();

        /* get the clone of the RPN stack for further evaluating */
        @SuppressWarnings("unchecked")
        QStack stackRPN = (QStack) this.stackRPN.clone();

        /* evaluating the RPN expression FIXME: add eval to value classes*/
        while (!stackRPN.empty()) {
            Object token = stackRPN.pop();
            if (token instanceof ArrayValue ) {
                ArrayValue val = (ArrayValue) token;
                stackAnswer.push( val.getEval() );
            } else
            if (token instanceof Value)
            {
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

        if (!valueArray && stackAnswer.size() > 1) {
            throw new QParseException("Missing or unknown operator:"+findNearesToken(stackAnswer));
        }

        return valueArray ? null : (RLSupplier<Value>) stackAnswer.pop();
    }

    private String findNearesToken(QStack stack) {
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


    public Object[] getAnswerAsArray() {
        return stackAnswer.toArray();
    }
}
