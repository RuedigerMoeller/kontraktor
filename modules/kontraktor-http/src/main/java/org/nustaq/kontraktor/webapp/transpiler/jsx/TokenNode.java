package org.nustaq.kontraktor.webapp.transpiler.jsx;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public abstract class TokenNode {
    protected StringBuilder chars = new StringBuilder(100); // content chars or js chars
    protected List<TokenNode> children = new ArrayList(); // tags

    public void add(char c) {
        if ( c > 0) {
            if ( chars.length() == 0 ) {
                ContentNode co = new ContentNode();
                addChild(co);
                co.chars = chars;
            }
            chars.append(c);
        }
    }

    public void addChild(TokenNode o) {
        children.add(o);
    }

    public void add(StringBuilder stringBuilder) {
        if ( chars.length() == 0 ) {
            ContentNode co = new ContentNode();
            addChild(co);
            co.chars = chars;
        }
        chars.append(stringBuilder);
    }

    public void add(String string) {
        if ( chars.length() == 0 ) {
            ContentNode co = new ContentNode();
            addChild(co);
            co.chars = chars;
        }
        chars.append(string);
    }

    public abstract void dump(PrintStream p, String indent);

    public void closeCont() {
        chars = new StringBuilder();
    }

    public StringBuilder getChars() {
        return chars;
    }

    public List<TokenNode> getChildren() {
        return children;
    }

}
