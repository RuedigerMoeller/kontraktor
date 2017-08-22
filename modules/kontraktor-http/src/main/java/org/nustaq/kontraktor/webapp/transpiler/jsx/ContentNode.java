package org.nustaq.kontraktor.webapp.transpiler.jsx;

import java.io.PrintStream;

public class ContentNode extends TokenNode {

    @Override
    public void dump(PrintStream p, String indent) {
        p.println(indent+"CNT:"+chars);
    }

    @Override
    public void add(char c) {
        chars.append(c);
    }

    public void add(StringBuilder stringBuilder) {
        chars.append(stringBuilder);
    }

    @Override
    public void closeCont() {
    }

    @Override
    public void addChild(TokenNode o) {
        throw new RuntimeException("just text allowed");
    }

    public void trim() {
        StringBuilder newChars = new StringBuilder(chars.length());
        boolean inWS = false;
        for (int i=0; i < chars.length(); i++ ) {
            char c = chars.charAt(i);
            if ( Character.isWhitespace(c) ) {
                if ( inWS ) {

                } else {
                    newChars.append(" ");
                    inWS = true;
                }
            } else {
                inWS = false;
                newChars.append(c);
            }
        }
        chars = newChars;
    }

    public boolean isEmpty() {
        return chars == null || chars.length() == 0 || ( chars.length() == 1 && Character.isWhitespace(chars.charAt(0)));
    }
}
