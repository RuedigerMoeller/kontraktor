package org.nustaq.kontraktor.apputil;

import org.nustaq.kontraktor.annotations.CallerSideMethod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public interface FileUtil {

    public static String RUN_ETC_TEMPLATES = "./run/etc/templates/";

    static String loadTemplate(String relPath, String ... keyvals ) throws IOException {
        String template = new String(Files.readAllBytes(Paths.get(RUN_ETC_TEMPLATES +relPath)), StandardCharsets.UTF_8);
        for (int i = 0; i < keyvals.length; i+=2) {
            String key = keyvals[i];
            String val = keyvals[i+1];
            template = template.replace("$"+key, val);
        }
        return template;
    }

}
