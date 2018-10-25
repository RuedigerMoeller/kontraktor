package org.nustaq.kontraktor.services;

import org.nustaq.kontraktor.util.Log;
import org.nustaq.kson.Kson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Config {
    static final String CLASSPATH_BASE = "/config/";
    final static Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_.-]+)(?::([^\\}]*))?\\}");

    final String filename;
    final boolean substituteEnvVars;
    final Kson kson;

    public Config(String filename, boolean substituteEnvVars, Class... configClasses) {
        this.filename = filename;
        this.substituteEnvVars = substituteEnvVars;
        this.kson = new Kson().map(configClasses);
    }

    public <ConfigType> ConfigType fromClasspath() throws Exception {
        String classpath = CLASSPATH_BASE + filename;

        URL resource = getClass().getResource(classpath);
        if(null == resource) {
            throw new FileNotFoundException("classpath:" + classpath);
        }
        return fromStream(getClass().getResourceAsStream(classpath));
    }

    public <ConfigType> ConfigType fromFilesystem(String dir) throws Exception {
        File f = new File(dir + "/" + filename);
        if(!f.exists()) {
            throw new FileNotFoundException(f.getAbsolutePath());
        }

        return fromStream(new FileInputStream(f));
    }

    <ConfigType> ConfigType fromStream(InputStream is) throws Exception {
        try {
            String encoding = "UTF-8";
            String fileContent = new Scanner(is, encoding).useDelimiter("\\A").next();

            if(substituteEnvVars) {
                Log.Info(Config.class, String.format("Substituting environment variables for '%s'", filename));
                fileContent = substitute(fileContent, System.getenv());
            }

            return (ConfigType) kson.readObject(fileContent);
        } finally {
            is.close();
        }
    }

    static String substitute(String toSubstitute, Map<String, ?> substitutions) {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = ENV_VAR_PATTERN.matcher(toSubstitute);
        int index = 0;

        while (matcher.find()) {
            sb.append(toSubstitute, index, matcher.start());
            String envVarName = matcher.group(1);
            Object obj = substitutions.get(envVarName);
            String value;
            if (obj != null) {
                value = String.valueOf(obj);
            } else {
                value = matcher.group(2);
                if (value == null)
                    value = "";
            }
            sb.append(value);
            index = matcher.end();
        }
        sb.append(toSubstitute, index, toSubstitute.length());

        return sb.toString();
    }
}
