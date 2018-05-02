package org.nustaq.kontraktor.webapp.npm;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class DumpVersions {

    public static String getVersion(String nodeModulePath) throws IOException {
        File pack = new File(nodeModulePath,"package.json");
        if ( pack.exists() ) {
            String version = null;
            JsonObject pjson = Json.parse(new FileReader(pack)).asObject();
            return pjson.getString("version", null);
        }
        return null;
    }

    public static void main(String[] args) {
        String dir = ".";
        if ( args == null || args.length > 0 ) {
            dir = args[0];
        }
        Arrays.stream(new File(dir).listFiles()).forEach( fi -> {
            try {
                String version = getVersion(fi.getAbsolutePath());
                if ( version != null ) {
                    System.out.println( "\""+fi.getName()+"\" : \""+version+"\"");
                } else {
                    System.out.println("no version "+fi.getName() );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
