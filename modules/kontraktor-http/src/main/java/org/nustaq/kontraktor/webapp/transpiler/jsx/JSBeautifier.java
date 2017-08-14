package org.nustaq.kontraktor.webapp.transpiler.jsx;

public class JSBeautifier {

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

    public StringBuilder readSlashComment(Inp in) {
        char c;
        StringBuilder res = new StringBuilder(100);
        while( (c=in.ch(0)) != 10 && c != 0)
        {
            res.append(c);
            in.advance(1);
        }
        return res;
    }

    StringBuilder readStarComment(Inp in) {
        char c;
        StringBuilder res = new StringBuilder(100);
        while( !((c=in.ch(-2)) == '*' && in.ch(-1)=='/') && c != 0)
        {
            res.append(c);
            in.advance(1);
        }
        return res;
    }

    StringBuilder readJSString(Inp in) {
        char endChar = in.ch(0);
        StringBuilder res = new StringBuilder(100);
        in.inc();res.append(endChar);
        while( in.ch() != endChar && in.ch(0) != 0 )
        {
            res.append(in.ch());
            in.advance(1);
            if (in.ch() == endChar && in.ch(-1)=='\\') {
                in.advance(1);
                res.append(in.ch());
            }
        }
        res.append(endChar);
        in.inc();
        return res;
    }

}
