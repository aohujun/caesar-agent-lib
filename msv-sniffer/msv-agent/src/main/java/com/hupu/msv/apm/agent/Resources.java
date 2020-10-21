package com.hupu.msv.apm.agent;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class Resources {
    private Resources() {
    }

    static File getResourceAsTempFile(String resourceName) throws IOException {

        File file = File.createTempFile(resourceName, ".tmp");
        OutputStream os = new FileOutputStream(file);
        try {
            getResourceAsTempFile(resourceName, file, os);
            return file;
        } finally {
            os.close();
        }
    }

    static void getResourceAsTempFile(String resourceName, File file, OutputStream os)
            throws IOException {
        file.deleteOnExit();

        InputStream is = getResourceAsStream(resourceName);
        try {
            int len = -1;
            while ((len = is.read()) != -1) {
                os.write(len);
            }
        } finally {
            is.close();
        }
    }

    private static InputStream getResourceAsStream(String resourceName) throws FileNotFoundException {
        InputStream is = Resources.class.getClassLoader().getResourceAsStream(resourceName);
        if (is == null) {
            throw new FileNotFoundException(
                    "Cannot find resource '" + resourceName + "' on the class path.");
        }
        return is;
    }
}