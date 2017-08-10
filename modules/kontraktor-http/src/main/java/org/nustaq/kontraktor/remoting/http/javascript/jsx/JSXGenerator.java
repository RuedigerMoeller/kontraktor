package org.nustaq.kontraktor.remoting.http.javascript.jsx;

import java.io.IOException;
import java.io.PrintStream;
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
                    out.print("    "+ae.getName()+":");
                    if ( ae.isJSValue() ) {
                        generateJS(ae,out);
                    } else {
                        out.print(ae.getValue());
                    }
                    if ( j < te.getAttributes().size()-1 )
                        out.println(",");
                    else
                        out.println("");
                }
                out.println( "  }"+(te.getChildren().size()>0?",":""));
            }
            for (int j = 0; j < te.getChildren().size(); j++) {
                TokenEntry entry = te.getChildren().get(j);
                if ( entry instanceof ContentEntry ) {
                    ContentEntry ce = (ContentEntry) entry;
                    ce.trim();
                    if ( ! ce.isEmpty() ) {
                        out.print("'");
                        renderSingleNode(entry, out);
                        out.print("'");
                    }
                } else
                    renderSingleNode(entry,out);
                if ( entry instanceof TagEntry ) {
                    if (j < te.getChildren().size() - 1)
                        out.println(",");
                }
            }
            out.print(")");
        } else {
            System.out.println("UNKNOWN NODE "+tokenEntry);
        }
    }

    public static void main(String[] args) throws IOException {
        JSXParser jsx = new JSXParser();
        JSEntry root = new JSEntry();
        byte[] bytes = Files.readAllBytes(Paths.get("/home/ruedi/IdeaProjects/kontraktor/examples/react/src/main/web/client/usertable.jsx"));
        jsx.parseJS(root,new Inp(new String(bytes,"UTF-8")));
//        root.dump(System.out,"");
        System.out.println();
        System.out.println();
        JSXGenerator gen = new JSXGenerator();
        gen.generateJS(root,System.out);
    }
}
