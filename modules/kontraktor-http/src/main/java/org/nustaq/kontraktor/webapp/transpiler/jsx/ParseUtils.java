package org.nustaq.kontraktor.webapp.transpiler.jsx;

import org.nustaq.kontraktor.webapp.transpiler.JSXIntrinsicTranspiler;
import org.nustaq.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public interface ParseUtils {

    Set WordCount = new HashSet();
    boolean CountWords = false;

    static int calcWordCount() {
        int sum = 0;
        for (Iterator iterator = WordCount.iterator(); iterator.hasNext(); ) {
            String next = (String) iterator.next();
            if ( next.startsWith("'") || next.startsWith("\""))
                next = next.substring(1);
            if ( next.endsWith("'") || next.endsWith("\""))
                next = next.substring(0,next.length()-1);
            if ( next.length() <= 3 || next.indexOf("-") >= 0 || next.indexOf("_") >= 0 || next.indexOf("/") >= 0 || next.startsWith("on") || next.indexOf("#") >= 0 || next.indexOf("calc") >= 0 )
                continue;
            int length = next.split(" ").length;
            if ( length < 1 )
                continue;
            sum += length;
            System.out.println(next);
        };
        return sum;
    }

    default StringBuilder readStarComment(Inp in) {
        StringBuilder res = new StringBuilder(100);
        while( !(in.ch(-2) == '*' && in.ch(-1)=='/') && in.ch() != 0)
        {
            res.append(in.ch());
            in.index++;
        }
        return res;
    }

    default StringBuilder readJSString(Inp in) {
        char endChar = in.ch();
        StringBuilder res = new StringBuilder(100);
        in.inc();res.append(endChar);
        while( in.ch() != endChar && in.ch() != 0 )
        {
            res.append(in.ch());
            in.advance(1);
            if (in.ch() == endChar && in.ch(-1)=='\\' && (in.ch(-2)!='\\' || in.ch(-3)=='\\' )) {
                in.advance(1);
                res.append(in.ch());
            }
        }
        res.append(endChar);
        in.inc();
        if ( CountWords && ! in.isNodeFile() ) {
            String s = res.toString();
            WordCount.add(s);
        }
        return res;
    }

    default StringBuilder readSlashComment(Inp in) {
        char c;
        StringBuilder res = new StringBuilder(100);
        while( (c=in.ch()) != 10 && c != 0)
        {
            res.append(c);
            in.advance(1);
        }
        return res;
    }

    // expects position at initial /
    default StringBuilder readRegexp(Inp in) {
        char c;
        in.index++;
        StringBuilder res = new StringBuilder(100);
        res.append('/');
        while( (c=in.ch()) != '/' || (c=='/' && Character.isDigit(in.ch(1))) )
        {
            res.append(c);
            in.index++;
            if ( c == '\\' ) {
                res.append(in.ch());
                in.index++;
            }
        }
        if ( in.ch(-1) == '/' ) // fix JSMin bug on regexp like '/tablet\//i'
        {
            res.append("_?");
        }
        in.index++;
        res.append('/');
        return res;
    }

    public static void main(String[] args) throws IOException {
        File f = new File("C:\\Users\\Moru0011\\IdeaProjects\\fundingbuero\\test.jsx");
        JSXParser jsx = new JSXParser(f,null);
        JSNode root = new JSNode();
        byte[] bytes = FileUtil.readFully(f);

        String cont = new String(bytes, "UTF-8");
        jsx.parseJS(root,new Inp(cont,f));
        if ( jsx.depth != 0 ) {
            System.out.println("POK "+jsx.depth);
        }
    }
}
