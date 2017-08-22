package org.nustaq.kontraktor.webapp.transpiler.jsx;

public class JSBeautifier implements ParseUtils {

    public void parseJS(GenOut cur, Inp in) {
        //FIXME; JS regexp
        char ch;
        while ( (ch=in.ch()) > 0)
        {
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
                switch (ch) {
                    case '}':
                    case ']':
                    case ')':
                        cur.unindent();
                        cur.print(ch);
                        break;
                    case '{':
                    case '[':
                    case '(':
                        cur.print(ch);
                        cur.indent();
                        break;
                    default:
                        cur.print(ch);
                }
                in.index++;
            }
        }
    }

}
