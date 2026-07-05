package com.smartparking.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class CryptoServiceTest {

    private CryptoService cryptoService;
    
    // 32-byte key in hex (64 characters)
    private final String testKeyHex = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f";

    @BeforeEach
    void setUp() {
        cryptoService = new CryptoService();
        ReflectionTestUtils.setField(cryptoService, "secretKeyHex", testKeyHex);
    }

    @Test
    void testEncryptDecryptSuccess() {
        String originalText = "30A-12345";
        
        String cipherText = cryptoService.encrypt(originalText);
        assertNotNull(cipherText);
        assertNotEquals(originalText, cipherText);

        String decryptedText = cryptoService.decrypt(cipherText);
        assertEquals(originalText, decryptedText);
    }

    @Test
    void testRandomIvProducesDifferentCiphertext() {
        String originalText = "30A-12345";
        
        String cipherText1 = cryptoService.encrypt(originalText);
        String cipherText2 = cryptoService.encrypt(originalText);
        
        assertNotNull(cipherText1);
        assertNotNull(cipherText2);
        // Random IV means identical inputs should encrypt to different outputs
        assertNotEquals(cipherText1, cipherText2);

        // But both should decrypt back to the same plain text
        assertEquals(originalText, cryptoService.decrypt(cipherText1));
        assertEquals(originalText, cryptoService.decrypt(cipherText2));
    }

    @Test
    void testHandlingNull() {
        assertNull(cryptoService.encrypt(null));
        assertNull(cryptoService.decrypt(null));
    }

    @Test
    void testInvalidKeyLength() {
        // Set invalid key hex length (too short)
        ReflectionTestUtils.setField(cryptoService, "secretKeyHex", "00010203");
        
        assertThrows(RuntimeException.class, () -> cryptoService.encrypt("some text"));
    }

    @Test
    void testDecryptMalformedText() {
        // Malformed base64
        assertThrows(RuntimeException.class, () -> cryptoService.decrypt("not-base64-string!!!"));
        
        // Base64 of a string that is too short to contain the 16-byte IV
        String tooShortBase64 = "YWJj"; // "abc"
        assertThrows(RuntimeException.class, () -> cryptoService.decrypt(tooShortBase64));
    }
}
