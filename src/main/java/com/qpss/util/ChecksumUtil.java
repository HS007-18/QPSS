package com.qpss.util;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ChecksumUtil {

    private ChecksumUtil() {}

    public static String calculateChecksum(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            return calculateChecksum(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to calculate checksum", e);
        }
    }

    public static String calculateChecksum(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            return calculateChecksum(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to calculate checksum", e);
        }
    }

    private static String calculateChecksum(InputStream is) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Failed to calculate checksum", e);
        }
    }
}
