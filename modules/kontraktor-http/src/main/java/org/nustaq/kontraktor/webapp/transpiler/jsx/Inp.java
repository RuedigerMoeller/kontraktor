package org.nustaq.kontraktor.webapp.transpiler.jsx;

import java.io.File;

public class Inp {
    char[] file;
    int index;
    int failcount =0;
    File f;

    public Inp(String s, File f) {
        index = 0;
        file = s.toCharArray();
        this.f = f;
    }

    public boolean isNodeFile() {
        return f != null && f.getAbsolutePath().indexOf("node_modules") >= 0;
    }

    public char ch(int off) {
        try {
            return file[index + off];
        } catch (Exception e) {
            failcount++;
            if ( failcount > 100 )
                throw new RuntimeException("prevent endlessloop, check for missing braces, tags or similar balanced chars");
            return 0;
        }
    }

    public void advance(int amount) {
        index+=amount;
    }

    public char ch() {
        try {
            return file[index];
        } catch (Exception e) {
            failcount++;
            if ( failcount > 100 )
                throw new RuntimeException("prevent endlessloop, check for missing braces, tags or similar balanced chars");
            return 0;
        }
    }

    public void inc() {
        index++;
    }

    public String toString() {
        int start = Math.max(0, index - 50);
        return new String(file, start,index-start);
    }

    public boolean match(String str) {
        for ( int i=0; i < str.length(); i++)
            if ( ch(i) != str.charAt(i))
                return false;
        return true;
    }

    public char peekNextNonWS(int off) {
        int curIndex = index;
        advance(off);
        skipWS();
        char res = ch();
        index = curIndex;
        return res;
    }

    public boolean matchReverse(String str) {
        for ( int i=0; i < str.length(); i++)
            if ( ch(-i) != str.charAt(str.length()-i-1))
                return false;
        return true;
    }

    public int index() {
        return index;
    }

    public String substring(int lastBracePo, int index) {
        return new String(file,lastBracePo,index-lastBracePo);
    }

    public char at(int lastBracePo) {
        if (lastBracePo<0)
            return 0;
        if (lastBracePo>=file.length)
            return 0;
        return file[lastBracePo];
    }

    public void skipWS() {
        while ( Character.isWhitespace(ch()) ) {
            inc();
        }
    }

    public char scanLastNWS() {
        int off = -1;
        while ( Character.isWhitespace(ch(off)) && off + index >= 0 )
        {
            off--;
        }
        return ch(off);
    }

    // last two nonws chars
    public String scanLastNWSDouble() {
        int off = -1;
        while ( Character.isWhitespace(ch(off)) && off + index >= 0 )
        {
            off--;
        }
        return ""+ch(off)+ch(off-1);
    }

    public boolean isFirstCharAfterLineBreak() {
        int off = -1;
        while ( Character.isWhitespace(ch(off)) && ch(off) != '\r' && ch(off) != '\n' && off + index >= 0 )
        {
            off--;
        }
        return  ch(off) == '\r' || ch(off) == '\n';
    }

    public StringBuffer readline() {
        StringBuffer line = new StringBuffer();
        while( ch() != '\r' && ch() != '\n' && ch() != 0 ) {
            line.append(ch());
            inc();
        }
        inc();
        if ( ch() == '\r' && ch() == '\n' )
            inc();
        return line;
    }

    public boolean isEOF() {
        return index >= file.length;
    }
}
