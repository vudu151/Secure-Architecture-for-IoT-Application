package com.smartparking.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
public class CryptoService {

    @Value("${aes.secret-key}")
    private String secretKeyHex;

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16;
    private final SecureRandom secureRandom = new SecureRandom();

    private SecretKeySpec getSecretKeySpec() {
        try {
            byte[] keyBytes = HexFormat.of().parseHex(secretKeyHex);
            if (keyBytes.length != 32) {
                throw new IllegalArgumentException("AES key must be 32 bytes (256 bits) for AES-256");
            }
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            log.error("Failed to parse AES secret key: ", e);
            throw new RuntimeException("AES Key configuration error", e);
        }
    }

    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKeySpec(), ivSpec);
            byte[] cipherTextBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Combine IV and Ciphertext
            byte[] combined = new byte[IV_SIZE + cipherTextBytes.length];
            System.arraycopy(iv, 0, combined, 0, IV_SIZE);
            System.arraycopy(cipherTextBytes, 0, combined, IV_SIZE, cipherTextBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Encryption failed: ", e);
            throw new RuntimeException("Encryption error", e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);
            if (combined.length < IV_SIZE) {
                throw new IllegalArgumentException("Invalid encrypted text: too short");
            }

            // Extract IV
            byte[] iv = new byte[IV_SIZE];
            System.arraycopy(combined, 0, iv, 0, IV_SIZE);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Extract Ciphertext
            int cipherTextLength = combined.length - IV_SIZE;
            byte[] cipherTextBytes = new byte[cipherTextLength];
            System.arraycopy(combined, IV_SIZE, cipherTextBytes, 0, cipherTextLength);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKeySpec(), ivSpec);
            byte[] plainTextBytes = cipher.doFinal(cipherTextBytes);

            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed: ", e);
            throw new RuntimeException("Decryption error", e);
        }
    }
}
