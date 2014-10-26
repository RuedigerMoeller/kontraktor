package org.nustaq.kontraktor.remoting.http;

import java.io.*;

/**
 * Created by ruedi on 26.10.14.
 *
 * A class merging a given list of js or html template snippets into a single file.
 * The location snippets/js files are looked up using the lookup path (similar to how
 * java looks up classpath for classes).
 *
 * This way only two http requests are required to load libraries and template snippets
 * for SPA applications without the need for client-side hacks.
 *
 */
public class ScriptComponentLoader {

    File resourcePath[];

    public ScriptComponentLoader setResourcePath( String ... path ) {
        resourcePath = new File[path.length];
        for (int i = 0; i < path.length; i++) {
            String dir = path[i];
            File f = new File( dir );
            if ( f.exists() && ! f.isDirectory() ) {
                throw new RuntimeException("only directorys can reside on resourcepath");
            }
            resourcePath[i] = f;
        }
        return this;
    }

    public File lookupResource( String finam ) {
        finam  = finam.replace('/',File.separatorChar);
        while ( finam.startsWith("/") )
            finam = finam.substring(1);
        for (int i = 0; i < resourcePath.length; i++) {
            File file = resourcePath[i];
            File loc = new File(file.getAbsolutePath() + File.separatorChar + finam);
            if ( loc.exists() ) {
                return loc;
            }
        }
        return null;
    }

    // very inefficient, however SPA's load once, so expect not too many requests to expect
    public byte[] mergeScripts( String ... jsFileNames ) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(2000);
        for (int i = 0; i < jsFileNames.length; i++) {
            String jsFileName = jsFileNames[i];
            File f = lookupResource(jsFileName);
            if ( f == null ) {
                System.out.println("unable to locate resource "+jsFileName);
            } else {
                byte[] bytes = new byte[(int) f.length()];
                try (FileInputStream fileInputStream = new FileInputStream(f)) {
                    fileInputStream.read(bytes);
                    bout.write(bytes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return bout.toByteArray();
    }

    // very inefficient, however SPA's load once, so expect not too many requests to expect
    public byte[] mergeTemplateSnippets( String ... templateFileNames ) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(2000);
        PrintStream pout = new PrintStream(bout);
        pout.println("document.write('\\");
        for (int i = 0; i < templateFileNames.length; i++) {
            String jsFileName = templateFileNames[i];
            File f = lookupResource(jsFileName);
            if ( f == null ) {
                System.out.println("unable to locate resource "+jsFileName);
            } else {
                try (FileReader fileInputStream = new FileReader(f)) {
                    BufferedReader in = new BufferedReader(fileInputStream);
                    while (in.ready()) {
                        String line = in.readLine();
                        line = line.replace("\'", "\\'");
                        pout.println(line+"\\");
                    }
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        pout.println("');");
        pout.flush();
        return bout.toByteArray();
    }

    public static void main( String arg[]) {
        ScriptComponentLoader loader = new ScriptComponentLoader().setResourcePath(
            ".",
            "/home/ruedi/IdeaProjects/abstractor/netty-kontraktor/src/main/webroot",
            "/home/ruedi/IdeaProjects/abstractor/src/main/javascript/js"
        );
        System.out.println(loader.lookupResource("index.html"));
        System.out.println(loader.lookupResource("index.html1"));
        System.out.println(loader.lookupResource("kontraktor.js"));

//        System.out.println( new String(loader.mergeScripts(
//            "jquery-2.1.1.js", "knockout3.2.0.js", "kontraktor.js"
//        )));
        System.out.println( new String(loader.mergeTemplateSnippets(
            "login.tpl.html"
        )));
    }

}
