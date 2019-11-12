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

    public QToken readNext() {
        int start = pos;
        while (true) {
            int ch = readChar();
            if (ch<0) {
                String substring = text.substring(start, pos);
                if ( substring.length() == 0 ) {
                    return null;
                }
                return new QToken(substring,text,pos);
            }
            if ( Character.isWhitespace(ch) ) {
                start++;
            } else if ( ch == '(' || ch == ')' ) {
                return new QToken(text.substring(start,pos),text,pos);
            } else if (Character.isJavaIdentifierStart(ch)) { // identifier
                ch=readChar();
                while( Character.isJavaIdentifierPart(ch) || ch == '.' ) {
                    ch=readChar();
                }
                if ( ch > 0 ) pos--;
                return new QToken(text.substring(start,pos),text,pos);
            } else if ( ch == '\"' ) { // string
                ch=readChar();
                while( ch > 0 && ch != '\"') {
                    ch=readChar();
                }
                return new QToken(text.substring(start,pos),text,pos);
            } else if ( ch == '\'' ) { // string
                ch=readChar();
                while( ch > 0 && ch != '\'') {
                    ch=readChar();
                }
                return new QToken(text.substring(start,pos),text,pos);
            } else if ( Character.isDigit(ch) || ch == '.' ) { // number
                ch=readChar();
                while( Character.isDigit(ch) || ch == '.' ) {
                    ch=readChar();
                }
                if ( ch > 0 ) pos--;
                return new QToken(text.substring(start,pos),text,pos);
            } else if ( !Character.isJavaIdentifierPart(ch) && ! Character.isWhitespace(ch) && ch != '\'' && ch != '\"' ) { // operator
                ch=readChar();
                while( !Character.isJavaIdentifierPart(ch) && ! Character.isWhitespace(ch) && ch > 0 && ch != '\'' && ch != '\"') {
                    ch=readChar();
                }
                if ( ch > 0 ) pos--;
                return new QToken(text.substring(start,pos),text,pos);
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

}
