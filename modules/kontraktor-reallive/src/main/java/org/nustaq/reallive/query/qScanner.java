package org.nustaq.reallive.query;

/**
 * Created by ruedi on 28.08.2015.
 */
public class QScanner {
    String text;
    int pos;

    public QScanner(String text) {
        this.text = text;
    }

    public String readNext() {
        int start = pos;
        while (true) {
            int ch = readChar();
            if (ch<0) {
                String substring = text.substring(start, pos);
                if ( substring.length() == 0 ) {
                    return null;
                }
                return substring;
            }
            if ( Character.isWhitespace(ch) ) {
                start++;
            } else if (Character.isJavaIdentifierStart(ch)) { // identifier
                ch=readChar();
                while( Character.isJavaIdentifierPart(ch) || ch == '.' ) {
                    ch=readChar();
                }
                if ( ch > 0 ) pos--;
                return text.substring(start,pos);
            } else if ( ch == '\'' ) { // string
                ch=readChar();
                while( ch > 0 && ch != '\'') {
                    ch=readChar();
                }
                return text.substring(start,pos);
            } else if ( Character.isDigit(ch) || ch == '.' ) { // number
                ch=readChar();
                while( Character.isDigit(ch) || ch == '.' ) {
                    ch=readChar();
                }
                if ( ch > 0 ) pos--;
                return text.substring(start,pos);
            } else if ( !Character.isJavaIdentifierPart(ch) && ! Character.isWhitespace(ch)) { // operator
                ch=readChar();
                while( !Character.isJavaIdentifierPart(ch) && ! Character.isWhitespace(ch) && ch > 0 ) {
                    ch=readChar();
                }
                if ( ch > 0 ) pos--;
                return text.substring(start,pos);
            } else {
                start++;
            }
        }
    }

    protected int readChar() {
        if ( pos >= text.length() )
            return -1;
        return text.charAt(pos++);
    }

    public static void main(String[] args) {
        QScanner sc = new QScanner(" x.y'pok'&&3 689 7765.876| || (3*7 +4)");
        String s;
        while ( (s=sc.readNext())!= null )
            System.out.println(s);
    }
}
