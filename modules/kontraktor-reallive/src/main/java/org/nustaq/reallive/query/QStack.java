package org.nustaq.reallive.query;

import java.util.ArrayList;
import java.util.EmptyStackException;

public class QStack extends ArrayList {

    public Object push(Object var1) {
        add(var1);
        return var1;
    }

    public Object pop() {
        int var2 = this.size();
        Object var1 = this.peek();
        this.remove(var2 - 1);
        return var1;
    }

    public Object peek() {
        int var1 = this.size();
        if (var1 == 0) {
            throw new EmptyStackException();
        } else {
            return this.get(var1 - 1);
        }
    }

    public boolean empty() {
        return this.size() == 0;
    }


    public Object lastElement() {
        return get(size()-1);
    }
}
