package com.seu.jdbcproxy.rewrite;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Encrypt {
    public static String encrypt(String plainText) throws Exception {
        //String plainText = "电力数据";
//        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
//        keyGenerator.init(128);
//        SecretKey secretKey = keyGenerator.generateKey();
        String secretKey = "this is password"; //16字节密钥，对应AES-128    AES-192 24字节  AES-256 32字节
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        String encryptedText =  Base64.getEncoder().encodeToString(encryptedBytes);
        return encryptedText;
    }

    public static void main(String[] args) throws Exception {
        String en_values = "电力数据";
        System.out.println(encrypt(en_values));
    }
}
