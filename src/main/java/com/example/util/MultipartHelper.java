package com.example.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class MultipartHelper {

        public static Map<String, byte[]> saveOnTomcat(HttpServletRequest request) throws Exception {
            Map<String, byte[]> files = new HashMap<>();

            String uploadDir = request.getServletContext().getRealPath("/WEB-INF/uploads");
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            for (Part part : request.getParts()) {
                if (part.getSubmittedFileName() != null) {
                    String filename = part.getSubmittedFileName();
                    File target = new File(dir, filename);

                    try (InputStream in = part.getInputStream()) {
                        Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    byte[] fileBytes = Files.readAllBytes(target.toPath());
                    files.put(filename, fileBytes);
                }
            }

            return files;
        }


    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        while ((nRead = is.read(data)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}
