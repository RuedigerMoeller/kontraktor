package org.nustaq.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileUtil {

    public static byte[] readFully(File f) throws IOException {
        try {
            return Files.readAllBytes(f.toPath().normalize());
        } catch (IOException e) {
            boolean b = f.exists();
            throw e;
        }
    }
}
