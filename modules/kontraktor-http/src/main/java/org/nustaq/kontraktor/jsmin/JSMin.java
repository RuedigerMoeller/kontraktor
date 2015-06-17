package org.nustaq.kontraktor.jsmin;
// original package ch.simschla.minify.js;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * This is a java port of Douglas Crockford's jsmin utility by https://github.com/simschla released under Apache 2.0 License
 * Currently implemented version: <a href="https://github.com/douglascrockford/JSMin/blob/2a943dba6bae746075749499b1da7955474a47b1/jsmin.c">2a943dba6bae746075749499b1da7955474a47b1</a>
 */
public class JSMin {

    private final InputStream inputStream;
    private final OutputStream outputStream;

    private final String customHeader;
    public static final int EOF = -1;

    private int theA;
    private int theB;
    private int theLookahead = EOF;
    private int theX = EOF;
    private int theY = EOF;

    private JSMin(Builder builder) {
        this.inputStream = builder.inputStream();
        this.outputStream = builder.outputStream();
        this.customHeader = builder.customHeader();
    }

    public void minify() {
        try {
            writeCustomHeader();
            jsmin();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                outputStream.flush();
            } catch (IOException e) {
                //ignore silently
            }
        }
    }

    protected void writeCustomHeader() throws IOException {
    }

    /* jsmin -- Copy the input to the output, deleting the characters which are
        insignificant to JavaScript. Comments will be removed. Tabs will be
        replaced with spaces. Carriage returns will be replaced with linefeeds.
        Most spaces and linefeeds will be removed.
    */
    private void jsmin() throws IOException {
        if (peek() == 0xEF) {
            get();
            get();
            get();
        }
        theA = '\n';
        action(3);
        while (theA != EOF) {
            switch (theA) {
                case ' ':
                    action(isAlphanum(theB) ? 1 : 2);
                    break;
                case '\n':
                    switch (theB) {
                        case '{':
                        case '[':
                        case '(':
                        case '+':
                        case '-':
                        case '!':
                        case '~':
                            action(1);
                            break;
                        case ' ':
                            action(3);
                            break;
                        default:
                            action(isAlphanum(theB) ? 1 : 2);
                    }
                    break;
                default:
                    switch (theB) {
                        case ' ':
                            action(isAlphanum(theA) ? 1 : 3);
                            break;
                        case '\n':
                            switch (theA) {
                                case '}':
                                case ']':
                                case ')':
                                case '+':
                                case '-':
                                case '"':
                                case '\'':
                                case '`':
                                    action(1);
                                    break;
                                default:
                                    action(isAlphanum(theA) ? 1 : 3);
                            }
                            break;
                        default:
                            action(1);
                            break;
                    }
            }
        }
    }

    /* get -- return the next character from input. Watch out for lookahead. If
        the character is a control character, translate it to a space or
        linefeed.
    */
    private int get() throws IOException {
        int c = theLookahead;
        theLookahead = EOF;
        if (c == EOF) {
            c = inputStream.read();
        }
        if (c >= ' ' || c == '\n' || c == EOF) {
            return c;
        }
        if (c == '\r') {
            return '\n';
        }
        return ' ';
    }

    /* peek -- get the next character without getting it.
    */
    private int peek() throws IOException {
        theLookahead = get();
        return theLookahead;
    }

    /* next -- get the next character, excluding comments. peek() is used to see
        if a '/' is followed by a '/' or '*'.
*/
    private int next() throws IOException {
        int c = get();
        if (c == '/') {
            switch (peek()) {
                case '/':
                    for (; ; ) {
                        c = get();
                        if (c <= '\n') {
                            break;
                        }
                    }
                    break;
                case '*':
                    get();
                    while (c != ' ') {
                        switch (get()) {
                            case '*':
                                if (peek() == '/') {
                                    get();
                                    c = ' ';
                                }
                                break;
                            case EOF:
                                error("Unterminated comment.");
                        }
                    }
                    break;
            }
        }
        theY = theX;
        theX = c;
        return c;
    }

	/* action -- do something! What you do is determined by the argument:
        1   Output A. Copy B to A. Get the next B.
        2   Copy B to A. Get the next B. (Delete A).
        3   Get the next B. (Delete B).
   action treats a string as a single character. Wow!
   action recognizes a regular expression if it is preceded by ( or , or =.
*/

    private void action(int d) throws IOException {
        int p;
        switch (d) {
            case 1:
                outputStream.write(theA);
                if (
                        (theY == '\n' || theY == ' ') &&
                                (theA == '+' || theA == '-' || theA == '*' || theA == '/') &&
                                (theB == '+' || theB == '-' || theB == '*' || theB == '/')
                        ) {
                    outputStream.write(theY);
                }
            case 2:
                theA = theB;
                if (theA == '\'' || theA == '"' || theA == '`') {
                    for (; ; ) {
                        outputStream.write(theA);
                        theA = get();
                        if (theA == theB) {
                            break;
                        }
                        if (theA == '\\') {
                            outputStream.write(theA);
                            theA = get();
                        }
                        if (theA == EOF) {
                            error("Unterminated string literal.");
                        }
                    }
                }
            case 3:
                theB = next();
                if (theB == '/' && (
                        theA == '(' || theA == ',' || theA == '=' || theA == ':' ||
                                theA == '[' || theA == '!' || theA == '&' || theA == '|' ||
                                theA == '?' || theA == '+' || theA == '-' || theA == '~' ||
                                theA == '*' || theA == '/' || theA == '\n'
                )) {
                    outputStream.write(theA);
                    if (theA == '/' || theA == '*') {
                        outputStream.write(' ');
                    }
                    outputStream.write(theB);
                    for (; ; ) {
                        theA = get();
                        if (theA == '[') {
                            for (; ; ) {
                                outputStream.write(theA);
                                theA = get();
                                if (theA == ']') {
                                    break;
                                }
                                if (theA == '\\') {
                                    outputStream.write(theA);
                                    theA = get();
                                }
                                if (theA == EOF) {
                                    error("Unterminated set in Regular Expression literal.");
                                }
                            }
                        } else if (theA == '/') {
                            switch (peek()) {
                                case '/':
                                case '*':
                                    error("Unterminated set in Regular Expression literal.");
                            }
                            break;
                        } else if (theA == '\\') {
                            outputStream.write(theA);
                            theA = get();
                        }
                        if (theA == EOF) {
                            error("Unterminated Regular Expression literal.");
                        }
                        outputStream.write(theA);
                    }
                    theB = next();
                }
        }
    }

    private void error(String s) {
        System.err.println("JSMin Error: " + s);
        throw new JsMinException(s);
    }

	/* isAlphanum -- return true if the character is a letter, digit, underscore,
        dollar sign, or non-ASCII character.
*/

    private boolean isAlphanum(int c) {
        return ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') ||
                (c >= 'A' && c <= 'Z') || c == '_' || c == '$' || c == '\\' ||
                c > 126);
    }


    public static Builder builder() {
        return new Builder();
    }

    //--- inner classes

    static final class JsMinException extends RuntimeException {
        private JsMinException(String message) {
            super(message);
        }
    }

    public static final class Builder {
        private InputStream inputStream = System.in;
        private OutputStream outputStream = System.out;

        private String customHeader = "";

        private Charset charset = Charset.forName("UTF-8");

        public InputStream inputStream() {
            return this.inputStream;
        }

        public OutputStream outputStream() {
            return this.outputStream;
        }

        public Builder inputStream(final InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        public Builder outputStream(final OutputStream outputStream) {
            this.outputStream = outputStream;
            return this;
        }

        public String customHeader() {
            return this.customHeader;
        }

        public Builder customHeader(final String customHeader) {
            this.customHeader = customHeader;
            return this;
        }

        public Charset charset() {
            return this.charset;
        }

        public Builder charset(final Charset charset) {
            this.charset = charset;
            return this;
        }


        public JSMin build() {
            return new JSMin(this);
        }
    }

    public static void main(String[] args) {
        JSMin.builder().build().minify();
    }
}
