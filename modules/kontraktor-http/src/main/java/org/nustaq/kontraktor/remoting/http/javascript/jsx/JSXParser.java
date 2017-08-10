package org.nustaq.kontraktor.remoting.http.javascript.jsx;

import org.jsoup.nodes.Document;
import org.jsoup.parser.ParseSettings;
import org.jsoup.parser.Parser;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class JSXParser {

    public static class ImportSpec {
        List<String> components = new ArrayList();
        List<String> aliases = new ArrayList();;
        String component;
        String alias;
        String from;

        public List<String> getComponents() {
            return components;
        }

        public List<String> getAliases() {
            return aliases;
        }

        public String getComponent() {
            return component;
        }

        public String getAlias() {
            return alias;
        }

        public String getFrom() {
            return from;
        }


        public ImportSpec components(List<String> components) {
            this.components = components;
            return this;
        }

        public ImportSpec aliases(List<String> aliases) {
            this.aliases = aliases;
            return this;
        }

        public ImportSpec component(String component) {
            this.component = component;
            return this;
        }

        public ImportSpec alias(String alias) {
            this.alias = alias;
            return this;
        }

        public ImportSpec from(String from) {
            this.from = from;
            return this;
        }

        @Override
        public String toString() {
            return "ImportSpec{" +
                "components=" + components +
                ", aliases=" + aliases +
                ", component='" + component + '\'' +
                ", alias='" + alias + '\'' +
                ", from='" + from + '\'' +
                '}';
        }
    }

    //fixme: cleanup and proper subclassing for parse nodes
    public static abstract class TokenEntry {
        protected StringBuilder chars = new StringBuilder(100); // content chars or js chars
        protected List<TokenEntry> children = new ArrayList(); // tags

        public void add(char c) {
            if ( c > 0) {
                if ( chars.length() == 0 ) {
                    ContentEntry co = new ContentEntry();
                    addChild(co);
                    co.chars = chars;
                }
                chars.append(c);
            }
        }

        public void addChild(TokenEntry o) {
            children.add(o);
        }

        public void add(StringBuilder stringBuilder) {
            if ( chars.length() == 0 ) {
                ContentEntry co = new ContentEntry();
                addChild(co);
                co.chars = chars;
            }
            chars.append(stringBuilder);
        }

        public abstract void dump(PrintStream p, String indent);

        public void closeCont() {
            if ( chars.toString().startsWith("</"))
            {
                int debug =1;
            }
            chars = new StringBuilder();
        }

        public StringBuilder getChars() {
            return chars;
        }

        public List<TokenEntry> getChildren() {
            return children;
        }

    }

    public static class JSEntry extends TokenEntry {

        public void dump(PrintStream p, String indent) {
            p.println(indent+"JS");
            for (int i = 0; i < children.size(); i++) {
                TokenEntry tokenEntry = children.get(i);
                tokenEntry.dump(p,indent+"  ");
            }
        }
    }

    public static class ContentEntry extends TokenEntry {

        @Override
        public void dump(PrintStream p, String indent) {
            p.println(indent+"CNT:"+chars);
        }

        @Override
        public void add(char c) {
            chars.append(c);
        }

        public void add(StringBuilder stringBuilder) {
            chars.append(stringBuilder);
        }

        @Override
        public void closeCont() {
        }

        @Override
        public void addChild(TokenEntry o) {
            throw new RuntimeException("just text allowed");
        }

        public void trim() {
            StringBuilder newChars = new StringBuilder(chars.length());
            boolean inWS = false;
            for (int i=0; i < chars.length(); i++ ) {
                char c = chars.charAt(i);
                if ( Character.isWhitespace(c) ) {
                    if ( inWS ) {

                    } else {
                        newChars.append(" ");
                        inWS = true;
                    }
                } else {
                    inWS = false;
                    newChars.append(c);
                }
            }
            chars = newChars;
        }

        public boolean isEmpty() {
            return chars == null || chars.length() == 0 || ( chars.length() == 1 && Character.isWhitespace(chars.charAt(0)));
        }
    }

    public static class TagEntry extends TokenEntry {

        protected StringBuilder tagName = new StringBuilder(100);
        List<AttributeEntry> attributes = new ArrayList(); // attrs if this is tag

        public void addTagName(char c) {
            if ( tagName == null )
                tagName = new StringBuilder(10);
            if ( c > 0)
                tagName.append(c);
        }

        public boolean isReactComponent() {
            return tagName != null && tagName.length() > 0 && Character.isUpperCase(tagName.charAt(0));
        }

        public StringBuilder getTagName() {
            return tagName;
        }

        public List<AttributeEntry> getAttributes() {
            return attributes;
        }

        public void addAttribute(AttributeEntry currentAttribute) {
            attributes.add(currentAttribute);
        }

        public void dump(PrintStream p, String indent) {
            p.println(indent+"_"+tagName+(children.size()==0?"/_":"_"));
            for (int i = 0; i < attributes.size(); i++) {
                AttributeEntry attributeEntry = attributes.get(i);
                attributeEntry.dump(p,indent+"  ");
            }
            for (int i = 0; i < children.size(); i++) {
                TokenEntry tokenEntry = children.get(i);
                tokenEntry.dump(p,indent+"  ");
            }
        }

    }

    public static class AttributeEntry extends TokenEntry {
        StringBuilder name;
        StringBuilder value;

        public AttributeEntry() {
        }

        public AttributeEntry name(StringBuilder name) {
            this.name = name;
            return this;
        }

        public AttributeEntry value(StringBuilder value) {
            this.value = value;
            return this;
        }

        public void dump(PrintStream p, String s) {
            p.println(s+"att:"+name+" val:"+value);
            if (children==null) return;
            for (int i = 0; i < children.size(); i++) {
                TokenEntry tokenEntry = children.get(i);
                tokenEntry.dump(p,"  "+s);
            }
        }

        public StringBuilder getName() {
            return name;
        }

        public StringBuilder getValue() {
            return value;
        }

        public boolean isJSValue() {
            return value == null && children.size() > 0 && children.get(0) instanceof JSEntry;
        }
    }

    public static class Inp {
        char[] file;
        int index;
        int failcount =0;
        public Inp(String s) {
            index = 0;
            file = s.toCharArray();
        }
        public char ch(int off) {
            if (index+off >= file.length) {
                failcount++;
                if ( failcount > 100 )
                    throw new RuntimeException("prevent endlessloop, check for missing braces, tags or similar balanced chars");
                return 0;
            }
            if (index+off < 0)
                return 0;
            return file[index+off];
        }

        public void advance(int amount) {
            index+=amount;
        }

        public char ch() {
            return ch(0);
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
    }

    List<ImportSpec> imports = new ArrayList();

    public void parseJS(TokenEntry cur, Inp in) {
        //FIXME; JS regexp
        int braceCount = 0;
        boolean returnOnLastMatchingBrace = in.ch() == '{';
        while (in.ch() > 0)
        {
            char ch = in.ch(0);
            if ( in.match("import ") && in.ch(-1) <= 32 ) {
                parseImport(in);
            } else
            if (in.ch(0) == '<' && Character.isLetter(in.ch(1))) {
                cur.closeCont();
                TagEntry tokenEntry = new TagEntry();
                cur.addChild(tokenEntry);
                parseJSX(tokenEntry,in);
            } else
            if ( ch == '/' && in.ch(1) == '/' ) {
                cur.add(readSlashComment(in));
            } else
            if ( ch == '"' || ch == '\'') {
                cur.add(readJSString(in));
            } else
            if ( ch == '/' && in.ch(1) == '*' ) {
                cur.add(readStarComment(in));
            } else
            {
                if ( returnOnLastMatchingBrace ) {
                    if (ch == '{')
                        braceCount++;
                    if (ch == '}') {
                        braceCount--;
                        if (braceCount < 0)
                            System.out.println("Warning: brace mismatch " + in);
                        if (braceCount <= 0) {
                            cur.add(ch);
                            in.advance(1);
                            cur.closeCont();
//                        if ( in.ch(1) == '}') // remove outer braces
//                            in.inc();
                            return;
                        }
                    }
                }
                cur.add(ch);
                in.advance(1);
            }
        }
    }

    void parseImport(Inp in) {
        in.advance("import ".length());
        ImportSpec spec = new ImportSpec();
        skipWS(in);
        if (in.ch()=='{') {
            in.inc();
            // list
            StringBuilder sb = new StringBuilder();
            while( in.ch() != '}' ) {
                sb.append(in.ch());
                in.inc();
            }
            in.inc();
            String[] split = sb.toString().split(",");
            for (int i = 0; i < split.length; i++) {
                String s = split[i].trim();
                if ( s.indexOf(" as ") >0 ) {
                    String[] as = s.split(" as ");
                    if ( as.length != 2 ) {
                        throw new RuntimeException("expected: 'X as Y':"+in);
                    }
                    spec.getComponents().add(as[0]);
                    spec.getAliases().add(as[1]);
                } else {
                    spec.getComponents().add(s);
                    spec.getAliases().add(s);
                }
            }
        } else {
            // plain
            StringBuilder sb = new StringBuilder();
            while ( !Character.isWhitespace(in.ch()) ) {
                sb.append(in.ch());
                in.inc();
            }
            skipWS(in);
            if ( in.match("from") ) {
                spec.component(sb.toString());
                spec.alias(sb.toString());
            } else { // as
                if ( ! in.match("as") )
                    throw new RuntimeException("expected 'as'");
                in.advance(2);
                skipWS(in);
                spec.component(sb.toString());
                StringBuilder al = new StringBuilder();
                while ( !Character.isWhitespace(in.ch()) ) {
                    al.append(in.ch());
                    in.inc();
                }
                spec.alias(al.toString());
            }
        }
        skipWS(in);
        if ( ! in.match("from") )
            throw new RuntimeException("expected from");
        in.advance(4);
        skipWS(in);
        StringBuilder src = readJSString(in);
        src.delete(0,1);
        src.setLength(src.length()-1);
        spec.from(src.toString());
        while( in.ch() >= 32) {
            in.inc();
        }
        while( in.ch() < 32 ) in.inc();
        System.out.println("IMPORT:"+spec);
        imports.add(spec);
    }

    public List<ImportSpec> getImports() {
        return imports;
    }

    void parseJSX(TagEntry tokenEntry, Inp in) {
        StringBuilder tag = parseTagName(in);
        tokenEntry.tagName = tag;
        boolean autoclosed = parseAttributesOrTEnd(tokenEntry, in);
        if ( ! autoclosed ) {
            parseJSXContent(tokenEntry,in);
        }
    }

    void parseJSXContent(TagEntry tag, Inp in) {
        while ( in.ch() > 0 ) {
            if ( in.ch() == '<' && in.ch(1) == '/' ) {
                // end tag
                in.advance(2);
                while( in.ch() != '>' && in.ch() > 0 )
                    in.inc();
                in.inc();
                tag.closeCont();
                return;
            } else
            if ( in.ch() == '<' && Character.isLetter(in.ch(1)) ) {
                tag.closeCont();
                TagEntry te = new TagEntry();
                parseJSX(te,in);
                tag.addChild(te);
            } else
            if (in.ch() == '{' ) {
                tag.closeCont();
                JSEntry js = new JSEntry();
                tag.addChild(js);
                parseJS(js,in);
            } else {
                tag.add(in.ch());
                in.inc();
            }
        }
    }

    boolean parseAttributesOrTEnd(TagEntry tokenEntry, Inp in) {
        while( in.ch(0) > 0 ) {
            skipWS(in);
            if (in.ch() == '/' && in.ch(1) == '>') // autoclose, no attrs
            {
                in.advance(2);
                return true;
            }
            if ( in.ch() == '>' ) {
                in.inc();
                return false;
            }
            StringBuilder name = readAttrName(in);
            skipWS(in);
            AttributeEntry attr = new AttributeEntry().name(name);
            tokenEntry.addAttribute(attr);
            if (in.ch() == '/' && in.ch(1) == '>') // autoclose, no value
            {
                return true;
            }
            skipWS(in);
            if (in.ch() == '=') {
                in.inc();
                readAttrValue(attr, in);
            }
        }
        return false;
    }

    private void skipWS(Inp in) {
        while ( Character.isWhitespace(in.ch()) ) {
            in.inc();
        }
    }

    private void readAttrValue(AttributeEntry attr, Inp in) {
        switch (in.ch()) {
            case '"':
            case '\'':
                attr.value(readJSString(in));
                break;
            case '{':
                JSEntry cur = new JSEntry();
                attr.addChild(cur);
                parseJS(cur,in);
                break;
            default:
                throw new RuntimeException("expect attribute value:"+in);
        }
    }

    private StringBuilder readAttrName(Inp in) {
        StringBuilder res = new StringBuilder(10);
        while( Character.isJavaIdentifierPart(in.ch(0)) ) {
            res.append(in.ch(0));
            in.advance(1);
        }
        return res;
    }

    StringBuilder parseTagName(Inp in) {
        in.advance(1);
        StringBuilder res = new StringBuilder(10);
        while( Character.isJavaIdentifierPart(in.ch(0)) ) {
            res.append(in.ch(0));
            in.advance(1);
        }
        return res;
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
            }
        }
        res.append(endChar);
        in.inc();
        return res;
    }

}
