package org.nustaq.kontraktor.remoting.http.javascript.jsx;

import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.Pair;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.nustaq.kontraktor.remoting.http.javascript.jsx.JSXParser.*;

public class JSXGenerator {

    public void generateJS( TokenEntry root, PrintStream out ) {
        for (int i = 0; i < root.getChildren().size(); i++) {
            TokenEntry tokenEntry = root.getChildren().get(i);
            renderSingleNode(tokenEntry, out);
        }
    }
    protected void renderSingleNode(TokenEntry tokenEntry, PrintStream out) {
        renderSingleNode(tokenEntry,out,false);
    }
    protected void renderSingleNode(TokenEntry tokenEntry, PrintStream out, boolean quote) {
        if ( tokenEntry instanceof JSEntry) {
            List<TokenEntry> chs = tokenEntry.getChildren();
            if ( chs.size() > 0 ) {
                if ( chs.get(0) instanceof ContentEntry )
                    ((ContentEntry) chs.get(0)).getChars().setCharAt(0,' ');
                if ( chs.get(chs.size() - 1) instanceof ContentEntry ) {
                    StringBuilder chars = ((ContentEntry) chs.get(chs.size() - 1)).getChars();
                    chars.setCharAt(chars.length() - 1, ' ');
                }
            }
            generateJS(tokenEntry,out);
        } else if ( tokenEntry instanceof ContentEntry) {
            if (quote)
                out.print(quoteJSString(tokenEntry.getChars()));
            else
                out.print(tokenEntry.getChars());
        } else if ( tokenEntry instanceof TagEntry) {
            TagEntry te = (TagEntry) tokenEntry;
            out.println("React.createElement(");
            if ( te.isReactComponent() ) {
                out.println("  "+te.getTagName()+",");
            } else {
                out.println("  '"+te.getTagName()+"',");
            }
            if ( te.getAttributes().size() == 0 ) {
                out.println("  null"+(te.getChildren().size()>0?",":""));
            } else {
                out.println( "  {");
                for (int j = 0; j < te.getAttributes().size(); j++) {
                    AttributeEntry ae = te.getAttributes().get(j);
                    out.print("    '"+ae.getName().toString()+"':");
                    if ( ae.isJSValue() ) {
                        generateJS(ae,out);
                    } else {
                        if (ae.getValue() != null ){
                            out.print(ae.getValue());
                        } else
                            out.println("true");
                    }
                    if ( j < te.getAttributes().size()-1 )
                        out.println(",");
                    else
                        out.println("");
                }
                out.println( "  }"+(te.getChildren().size()>0?",":""));
            }
            boolean nonEmptyWasThere = false;
            for (int j = 0; j < te.getChildren().size(); j++) {
                TokenEntry entry = te.getChildren().get(j);
                if ( entry instanceof ContentEntry ) {
                    ContentEntry ce = (ContentEntry) entry;
                    ce.trim();
                    if ( ! ce.isEmpty() ) {
                        if ( nonEmptyWasThere )
                            out.println(",");
                        nonEmptyWasThere = true;
                        out.print("'");
                        renderSingleNode(entry, out, true);
                        out.print("'");
                    }
                } else {
                    if ( nonEmptyWasThere )
                        out.println(",");
                    nonEmptyWasThere = true;
                    renderSingleNode(entry, out);
                }
            }
            out.print(")");
        } else {
            System.out.println("UNKNOWN NODE "+tokenEntry);
        }
    }

    private String quoteJSString(StringBuilder value) {
        String s = value.toString();
        s = s.replace("\"","\\\"");
        s = s.replace("'","\\'");
        return s;
    }

    public static String camelCase(String name) {
        int i = name.indexOf("-");
        if ( i > 0) {
            return camelCase(name.substring(0,i)+Character.toUpperCase(name.charAt(i+1))+name.substring(i+2));
        }
        return name;
    }

    public static class ParseResult {
        byte[] filedata;
        List<ImportSpec> imports;
        List<String> globals;

        public ParseResult(byte[] filedata, List<ImportSpec> imports, List<String> globals) {
            this.filedata = filedata;
            this.imports = imports;
            this.globals = globals;
        }

        public byte[] getFiledata() {
            return filedata;
        }

        public List<ImportSpec> getImports() {
            return imports;
        }

        public List<String> getGlobals() {
            return globals;
        }

    }

    public static ParseResult process(File f) throws IOException {
        JSXParser jsx = new JSXParser();
        JSEntry root = new JSEntry();
        byte[] bytes = Files.readAllBytes(f.toPath());

        jsx.parseJS(root,new Inp(new String(bytes,"UTF-8")));
        if ( jsx.depth != 0 ) {
            Log.Warn(JSXGenerator.class,"probably parse issues non-matching braces in "+f.getAbsolutePath());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
        PrintStream ps = new PrintStream(out);
        JSXGenerator gen = new JSXGenerator();
        gen.generateJS(root,ps);
        ps.flush();
        ps.close();
        return new ParseResult(out.toByteArray(),jsx.getImports(),jsx.getTopLevelObjects());
    }
}
