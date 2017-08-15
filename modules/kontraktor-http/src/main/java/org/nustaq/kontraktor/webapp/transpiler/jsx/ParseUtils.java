package org.nustaq.kontraktor.webapp.transpiler.jsx;

public interface ParseUtils {

    default StringBuilder readStarComment(Inp in) {
        StringBuilder res = new StringBuilder(100);
        while( !(in.ch(-2) == '*' && in.ch(-1)=='/') && in.ch() != 0)
        {
            res.append(in.ch());
            in.advance(1);
        }
        return res;
    }

    default StringBuilder readJSString(Inp in) {
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

    default StringBuilder readSlashComment(Inp in) {
        char c;
        StringBuilder res = new StringBuilder(100);
        while( (c=in.ch(0)) != 10 && c != 0)
        {
            res.append(c);
            in.advance(1);
        }
        return res;
    }

}
