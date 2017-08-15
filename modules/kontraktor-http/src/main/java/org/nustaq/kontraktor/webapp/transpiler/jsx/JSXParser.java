package org.nustaq.kontraktor.webapp.transpiler.jsx;

import java.io.PrintStream;
import java.util.*;

public class JSXParser implements ParseUtils {

    public static boolean SHIM_OBJ_SPREAD = true;

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

        public void add(String string) {
            if ( chars.length() == 0 ) {
                ContentEntry co = new ContentEntry();
                addChild(co);
                co.chars = chars;
            }
            chars.append(string);
        }

        public abstract void dump(PrintStream p, String indent);

        public void closeCont() {
            if ( chars.toString().startsWith("/>"))
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

    List<ImportSpec> imports = new ArrayList();
    List<String> topLevelObjects = new ArrayList();

    public List<String> getTopLevelObjects() {
        return topLevelObjects;
    }

    int depth = 0;
    public void parseJS(TokenEntry cur, Inp in) {
        //FIXME; JS regexp
        int braceCount = 0;
        boolean trackNextIdentifier = false;
        StringBuilder global = new StringBuilder();
        boolean returnOnLastMatchingBrace = in.ch() == '{';
        int lastBracePos[] = new int[200];
        int lastBracePosCur[] = new int[200];
        String braceInsert[] = new String[200];
        int rcnt = 0;
        while (in.ch() > 0)
        {
            char ch = in.ch(0);
            if ( in.match("export") && in.ch(-1) <= 32 ) {
                in.advance("export".length());
                in.skipWS();
                continue;
            }
            if ( in.match("import ") && in.ch(-1) <= 32 ) {
                parseImport(in);
            } else
            if (in.ch(0) == '<' && Character.isLetter(in.ch(1))) {
                cur.closeCont();
                TagEntry tokenEntry = new TagEntry();
                cur.addChild(tokenEntry);
                depth++;
                parseJSX(tokenEntry,in);
                depth--;
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
                if ( SHIM_OBJ_SPREAD && in.match("...") && in.at(lastBracePos[depth]) == '{' ) {
//                    System.out.println(in.substring(lastBracePos[depth],in.index()));
//                    System.out.println(cur.chars.toString().substring(lastBracePosCur[depth]));
                    if ( braceInsert[depth] == null ) {
                        cur.chars.insert(lastBracePosCur[depth], "_sprd(");
                        // fixme: correct indices ?
//                        System.out.println(cur.chars.toString().substring(lastBracePosCur[depth]));
                        braceInsert[depth] = ")";
                    }
                    cur.add("'..."+(rcnt++)+"':");
                    in.advance(3);
                    continue;
                }
                if (ch == '{' || ch == '[' || ch == '(') {
                    depth++;
                    lastBracePos[depth] = in.index();
                    lastBracePosCur[depth] = cur.chars.length();
                }
                if (ch == '}' || ch == ']' || ch == ')' ) {
                    depth--;
                }
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
                if ( depth == 0 && !Character.isLetterOrDigit(in.ch(-1)) ) {
                    if ( in.match("var ") || in.match("let ") ) {
                        for (int i=0; i < 4; i++ ) {
                            cur.add(in.ch());
                            in.inc();
                        }
                        trackNextIdentifier = true;
                        continue;
                    }
                    if ( in.match("function ") ) {
                        int length = "function ".length();
                        for (int i = 0; i < length; i++ ) {
                            cur.add(in.ch());
                            in.inc();
                        }
                        trackNextIdentifier = true;
                        continue;
                    }
                    if ( in.match("const ") || in.match("class ") ) {
                        for (int i=0; i < 5; i++ ) {
                            cur.add(in.ch());
                            in.inc();
                        }
                        trackNextIdentifier = true;
                        continue;
                    }
                }
                if ( trackNextIdentifier ) {
                    if (Character.isJavaIdentifierPart(ch) ) {
                        global.append(ch);
                    } else {
                        if ( global.length() > 0 ) {
                            topLevelObjects.add(global.toString());
                            global.setLength(0);
                            trackNextIdentifier = false;
                        }
                    }
                }
                cur.add(ch);
                if (ch == '}') {
                    if ( braceInsert[depth+1] != null ) {
                        cur.chars.append(braceInsert[depth+1]);
                        braceInsert[depth+1] = null;
                    }
                }
                in.advance(1);
            }
        }
    }

    void parseImport(Inp in) {
        in.advance("import ".length());
        ImportSpec spec = new ImportSpec();
        while (in.ch()>0) {
            in.skipWS();
            if (in.ch() == '{') {
                in.inc();
                // list
                StringBuilder sb = new StringBuilder();
                while (in.ch() != '}') {
                    sb.append(in.ch());
                    in.inc();
                }
                in.inc();
                String[] split = sb.toString().split(",");
                for (int i = 0; i < split.length; i++) {
                    String s = split[i].trim();
                    if (s.indexOf(" as ") > 0) {
                        String[] as = s.split(" as ");
                        if (as.length != 2) {
                            throw new RuntimeException("expected: 'X as Y':" + in);
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
                while (isAttrNamePart(in.ch())) {
                    sb.append(in.ch());
                    in.inc();
                }
                in.skipWS();
                if (in.match("as ")) { // as
                    in.advance(2);
                    in.skipWS();
                    spec.component(sb.toString());
                    StringBuilder al = new StringBuilder();
                    while (!Character.isWhitespace(in.ch())) {
                        al.append(in.ch());
                        in.inc();
                    }
                    spec.alias(al.toString());
                }
            }
            in.skipWS();
            if ( in.ch() == ',' )
            {
                in.inc();
                in.skipWS();
            } else {
                break;
            }
        }
        in.skipWS();
        if ( ! in.match("from") )
            throw new RuntimeException("expected from >"+in+"<");
        in.advance(4);
        in.skipWS();
        StringBuilder src = readJSString(in);
        src.delete(0,1);
        src.setLength(src.length()-1);
        spec.from(src.toString());
        while( in.ch() >= 32) {
            in.inc();
        }
        while( in.ch() < 32 ) in.inc();

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
            if (in.ch() == '&' ) { //entity
                StringBuilder ent = new StringBuilder();
                int off = 1;
                while ( off < 20 && in.ch(off) != ';') {
                    ent.append(in.ch(off));
                    off++;
                }
                if ( in.ch(off) == ';' ) {
                    Character character = entityMap.get(ent.toString());
                    if ( character == null && in.ch(1) == '#') {
                        try {
                            if (in.ch(2) == 'x') {
                                character = (char) Integer.parseInt(ent.toString().substring(2), 16);
                            } else {
                                character = (char) Integer.parseInt(ent.toString().substring(2));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if ( character != null ) {
                        tag.add(character);
                        in.advance(off + 1);
                        continue;
                    }
                }
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
            in.skipWS();
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
            in.skipWS();
            AttributeEntry attr = new AttributeEntry().name(name);
            tokenEntry.addAttribute(attr);
            if (in.ch() == '/' && in.ch(1) == '>') // autoclose, no value
            {
                in.advance(2);
                return true;
            }
            in.skipWS();
            if (in.ch() == '=') {
                in.inc();
                readAttrValue(attr, in);
            }
            if ( tokenEntry.attributes.size() > 100 )
                throw new RuntimeException("parsing error:"+in);
        }
        return false;
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
        while( isAttrNamePart(in.ch()) ) {
            res.append(in.ch(0));
            in.advance(1);
        }
        return res;
    }

    boolean isAttrNamePart(char ch) {
        return Character.isJavaIdentifierPart(ch) || ch == '-';
    }

    StringBuilder parseTagName(Inp in) {
        in.advance(1);
        StringBuilder res = new StringBuilder(10);
        while( isAttrNamePart(in.ch(0)) ) {
            res.append(in.ch(0));
            in.advance(1);
        }
        return res;
    }

    static Object entities[] = {
        "quot", '\u0022',
        "amp", '&',
        "apos", '\'',
        "lt", '<',
        "gt", '>',
        "nbsp", '\u00A0',
        "iexcl", '\u00A1',
        "cent", '\u00A2',
        "pound", '\u00A3',
        "curren", '\u00A4',
        "yen", '\u00A5',
        "brvbar", '\u00A6',
        "sect", '\u00A7',
        "uml", '\u00A8',
        "copy", '\u00A9',
        "ordf", '\u00AA',
        "laquo", '\u00AB',
        "not", '\u00AC',
        "shy", '\u00AD',
        "reg", '\u00AE',
        "macr", '\u00AF',
        "deg", '\u00B0',
        "plusmn", '\u00B1',
        "sup2", '\u00B2',
        "sup3", '\u00B3',
        "acute", '\u00B4',
        "micro", '\u00B5',
        "para", '\u00B6',
        "middot", '\u00B7',
        "cedil", '\u00B8',
        "sup1", '\u00B9',
        "ordm", '\u00BA',
        "raquo", '\u00BB',
        "frac14", '\u00BC',
        "frac12", '\u00BD',
        "frac34", '\u00BE',
        "iquest", '\u00BF',
        "Agrave", '\u00C0',
        "Aacute", '\u00C1',
        "Acirc", '\u00C2',
        "Atilde", '\u00C3',
        "Auml", '\u00C4',
        "Aring", '\u00C5',
        "AElig", '\u00C6',
        "Ccedil", '\u00C7',
        "Egrave", '\u00C8',
        "Eacute", '\u00C9',
        "Ecirc", '\u00CA',
        "Euml", '\u00CB',
        "Igrave", '\u00CC',
        "Iacute", '\u00CD',
        "Icirc", '\u00CE',
        "Iuml", '\u00CF',
        "ETH", '\u00D0',
        "Ntilde", '\u00D1',
        "Ograve", '\u00D2',
        "Oacute", '\u00D3',
        "Ocirc", '\u00D4',
        "Otilde", '\u00D5',
        "Ouml", '\u00D6',
        "times", '\u00D7',
        "Oslash", '\u00D8',
        "Ugrave", '\u00D9',
        "Uacute", '\u00DA',
        "Ucirc", '\u00DB',
        "Uuml", '\u00DC',
        "Yacute", '\u00DD',
        "THORN", '\u00DE',
        "szlig", '\u00DF',
        "agrave", '\u00E0',
        "aacute", '\u00E1',
        "acirc", '\u00E2',
        "atilde", '\u00E3',
        "auml", '\u00E4',
        "aring", '\u00E5',
        "aelig", '\u00E6',
        "ccedil", '\u00E7',
        "egrave", '\u00E8',
        "eacute", '\u00E9',
        "ecirc", '\u00EA',
        "euml", '\u00EB',
        "igrave", '\u00EC',
        "iacute", '\u00ED',
        "icirc", '\u00EE',
        "iuml", '\u00EF',
        "eth", '\u00F0',
        "ntilde", '\u00F1',
        "ograve", '\u00F2',
        "oacute", '\u00F3',
        "ocirc", '\u00F4',
        "otilde", '\u00F5',
        "ouml", '\u00F6',
        "divide", '\u00F7',
        "oslash", '\u00F8',
        "ugrave", '\u00F9',
        "uacute", '\u00FA',
        "ucirc", '\u00FB',
        "uuml", '\u00FC',
        "yacute", '\u00FD',
        "thorn", '\u00FE',
        "yuml", '\u00FF',
        "OElig", '\u0152',
        "oelig", '\u0153',
        "Scaron", '\u0160',
        "scaron", '\u0161',
        "Yuml", '\u0178',
        "fnof", '\u0192',
        "circ", '\u02C6',
        "tilde", '\u02DC',
        "Alpha", '\u0391',
        "Beta", '\u0392',
        "Gamma", '\u0393',
        "Delta", '\u0394',
        "Epsilon", '\u0395',
        "Zeta", '\u0396',
        "Eta", '\u0397',
        "Theta", '\u0398',
        "Iota", '\u0399',
        "Kappa", '\u039A',
        "Lambda", '\u039B',
        "Mu", '\u039C',
        "Nu", '\u039D',
        "Xi", '\u039E',
        "Omicron", '\u039F',
        "Pi", '\u03A0',
        "Rho", '\u03A1',
        "Sigma", '\u03A3',
        "Tau", '\u03A4',
        "Upsilon", '\u03A5',
        "Phi", '\u03A6',
        "Chi", '\u03A7',
        "Psi", '\u03A8',
        "Omega", '\u03A9',
        "alpha", '\u03B1',
        "beta", '\u03B2',
        "gamma", '\u03B3',
        "delta", '\u03B4',
        "epsilon", '\u03B5',
        "zeta", '\u03B6',
        "eta", '\u03B7',
        "theta", '\u03B8',
        "iota", '\u03B9',
        "kappa", '\u03BA',
        "lambda", '\u03BB',
        "mu", '\u03BC',
        "nu", '\u03BD',
        "xi", '\u03BE',
        "omicron", '\u03BF',
        "pi", '\u03C0',
        "rho", '\u03C1',
        "sigmaf", '\u03C2',
        "sigma", '\u03C3',
        "tau", '\u03C4',
        "upsilon", '\u03C5',
        "phi", '\u03C6',
        "chi", '\u03C7',
        "psi", '\u03C8',
        "omega", '\u03C9',
        "thetasym", '\u03D1',
        "upsih", '\u03D2',
        "piv", '\u03D6',
        "ensp", '\u2002',
        "emsp", '\u2003',
        "thinsp", '\u2009',
        "zwnj", '\u200C',
        "zwj", '\u200D',
        "lrm", '\u200E',
        "rlm", '\u200F',
        "ndash", '\u2013',
        "mdash", '\u2014',
        "lsquo", '\u2018',
        "rsquo", '\u2019',
        "sbquo", '\u201A',
        "ldquo", '\u201C',
        "rdquo", '\u201D',
        "bdquo", '\u201E',
        "dagger", '\u2020',
        "Dagger", '\u2021',
        "bull", '\u2022',
        "hellip", '\u2026',
        "permil", '\u2030',
        "prime", '\u2032',
        "Prime", '\u2033',
        "lsaquo", '\u2039',
        "rsaquo", '\u203A',
        "oline", '\u203E',
        "frasl", '\u2044',
        "euro", '\u20AC',
        "image", '\u2111',
        "weierp", '\u2118',
        "real", '\u211C',
        "trade", '\u2122',
        "alefsym", '\u2135',
        "larr", '\u2190',
        "uarr", '\u2191',
        "rarr", '\u2192',
        "darr", '\u2193',
        "harr", '\u2194',
        "crarr", '\u21B5',
        "lArr", '\u21D0',
        "uArr", '\u21D1',
        "rArr", '\u21D2',
        "dArr", '\u21D3',
        "hArr", '\u21D4',
        "forall", '\u2200',
        "part", '\u2202',
        "exist", '\u2203',
        "empty", '\u2205',
        "nabla", '\u2207',
        "isin", '\u2208',
        "notin", '\u2209',
        "ni", '\u220B',
        "prod", '\u220F',
        "sum", '\u2211',
        "minus", '\u2212',
        "lowast", '\u2217',
        "radic", '\u221A',
        "prop", '\u221D',
        "infin", '\u221E',
        "ang", '\u2220',
        "and", '\u2227',
        "or", '\u2228',
        "cap", '\u2229',
        "cup", '\u222A',
        "'int'", '\u222B',
        "there4", '\u2234',
        "sim", '\u223C',
        "cong", '\u2245',
        "asymp", '\u2248',
        "ne", '\u2260',
        "equiv", '\u2261',
        "le", '\u2264',
        "ge", '\u2265',
        "sub", '\u2282',
        "sup", '\u2283',
        "nsub", '\u2284',
        "sube", '\u2286',
        "supe", '\u2287',
        "oplus", '\u2295',
        "otimes", '\u2297',
        "perp", '\u22A5',
        "sdot", '\u22C5',
        "lceil", '\u2308',
        "rceil", '\u2309',
        "lfloor", '\u230A',
        "rfloor", '\u230B',
        "lang", '\u2329',
        "rang", '\u232A',
        "loz", '\u25CA',
        "spades", '\u2660',
        "clubs", '\u2663',
        "hearts", '\u2665',
        "diams", '\u2666'
    };

    public static Map<String,Character> entityMap = new HashMap();
    static {
        for (int i = 0; i < entities.length; i+=2) {
            Object entity = entities[i];
            entityMap.put((String)entity,(Character)entities[i+1]);
        }
    }
}
