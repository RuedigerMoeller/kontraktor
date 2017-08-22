package org.nustaq.kontraktor.webapp.transpiler.jsx;

import java.io.PrintStream;

public class JSNode extends TokenNode {

    public void dump(PrintStream p, String indent) {
        p.println(indent+"JS");
        for (int i = 0; i < children.size(); i++) {
            TokenNode tokenNode = children.get(i);
            tokenNode.dump(p,indent+"  ");
        }
    }
}
