package com.seu.jdbcproxy;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EncryptTests {
    private static final Logger logger = LoggerFactory.getLogger(EncryptTests.class);

    @Test
    public void aesEncrypt() throws Exception {
        String plainText = "SELECT * FROM my_table";
//        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
//        keyGenerator.init(128);
//        SecretKey secretKey = keyGenerator.generateKey();
        String secretKey = "this is fixed test key password."; //16字节密钥，对应AES-128    AES-192 24字节  AES-256 32字节
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        String encryptedText =  Base64.getEncoder().encodeToString(encryptedBytes);
        System.out.println("AES加密后的内容= " + encryptedText);
    }

    @Test
    public void aesDecrypt() throws Exception {
        String encryptedText = "yCO54+UusmHRrm5tXEIudLPc0wTkLDLRgWYvJHvMLmA=";
        String secretKey = "this is fixed test key password.";
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        String plainText = new String(decryptedBytes, StandardCharsets.UTF_8);
        System.out.println("AES解密的内容= " + plainText);

    }
}
