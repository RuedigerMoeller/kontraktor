package org.nustaq.kontraktor.webapp.transpiler.jsx;

import java.io.PrintStream;

public class GenOut {
    final int NOINDENT = 0;
    final int AFTERLF = 1;

    int state = NOINDENT;
    PrintStream ps;
    private String indent = "";

    public GenOut(PrintStream ps) {
        this.ps = ps;
    }

    public void flush() {
        ps.flush();
    }

    public void close() {
        ps.close();
    }

    public void print(String s) {
        for ( int i = 0; i < s.length(); i++)
            print(s.charAt(i));
    }

    public void println(String s) {
        print(s);
        print('\n');
    }

    public void print(StringBuilder s) {
        for ( int i = 0; i < s.length(); i++)
            print(s.charAt(i));
    }

    public void indent() {
        indent+="  ";
    }

    public void unindent() {
        if ( indent.length() >= 2 )
            indent = indent.substring(0,indent.length()-2);
    }

    public void println(StringBuilder stringBuilder) {
        print(stringBuilder);
        print('\n');
    }

    public void print(char ch) {
        switch (state){
            case NOINDENT:
                ps.print(ch);
                if ( ch == '\n' ) {
                    state = AFTERLF;
                }
                break;
            case AFTERLF:
                if ( !Character.isWhitespace(ch) || ch == '\n' ) {
                    ps.print(indent);
                    ps.print(ch);
                    if ( ch != '\n' )
                        state = NOINDENT;
                }
                break;
        }
    }
}
