package org.nustaq.kontraktor.webapp.transpiler.jsx;

public class Inp {
    char[] file;
    int index;
    int failcount =0;
    public Inp(String s) {
        index = 0;
        file = s.toCharArray();
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
}
