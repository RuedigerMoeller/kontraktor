package org.nustaq.kontraktor.webapp.transpiler.jsx;

import java.io.PrintStream;

public class AttributeNode extends TokenNode {
    StringBuilder name;
    StringBuilder value;

    public AttributeNode() {
    }

    public AttributeNode name(StringBuilder name) {
        this.name = name;
        return this;
    }

    public AttributeNode value(StringBuilder value) {
        this.value = value;
        return this;
    }

    public void dump(PrintStream p, String s) {
        p.println(s+"att:"+name+" val:"+value);
        if (children==null) return;
        for (int i = 0; i < children.size(); i++) {
            TokenNode tokenNode = children.get(i);
            tokenNode.dump(p,"  "+s);
        }
    }

    public StringBuilder getName() {
        return name;
    }

    public StringBuilder getValue() {
        return value;
    }

    public boolean isJSValue() {
        return value == null && children.size() > 0 && children.get(0) instanceof JSNode;
    }
}
