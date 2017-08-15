package org.nustaq.kontraktor.webapp.transpiler.jsx;

public class JSBeautifier implements ParseUtils {

    public void parseJS(GenOut cur, Inp in) {
        //FIXME; JS regexp
        boolean returnOnLastMatchingBrace = in.ch() == '{';
        while (in.ch() > 0)
        {
            char ch = in.ch(0);
            if ( ch == '/' && in.ch(1) == '/' ) {
                cur.print(readSlashComment(in));
            } else
            if ( ch == '"' || ch == '\'') {
                cur.print(readJSString(in));
            } else
            if ( ch == '/' && in.ch(1) == '*' ) {
                cur.print(readStarComment(in));
            } else
            {
                if (ch == '}' || ch == ']' || ch == ')' ) {
                    cur.unindent();
                }
                cur.print(ch);
                if (ch == '{' || ch == '[' || ch == '(') {
                    cur.indent();
                }
                in.inc();
            }
        }
    }

}
