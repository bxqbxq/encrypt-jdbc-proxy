package com.seu.jdbcproxy.rewrite;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EncryptionHelper {
    //16字节密钥，对应AES-128    AES-192 24字节  AES-256 32字节
    public static final String KEY = "this is password";
    public static final String Algorithm = "AES";

    public SecretKeySpec secretKey;

    public EncryptionHelper() {
        secretKey = new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), Algorithm);  //StandardCharsets.UTF_8
    }

    public String encrypt(String plainText) throws Exception {

        Cipher cipher = Cipher.getInstance(Algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        String encryptedText =  Base64.getEncoder().encodeToString(encryptedBytes);

        return encryptedText;
    }

    public String decrypt(String encryptedText) throws Exception {

        if (!isCipherText(encryptedText)){
            return encryptedText;
        }

        Cipher cipher = Cipher.getInstance(Algorithm);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));

        String plainText = new String(decryptedBytes, StandardCharsets.UTF_8);
        return plainText;


    }

    public boolean isCipherText(String text) {
        try {
            byte[] decoded = Base64.getDecoder().decode(text);
            String reencoded = Base64.getEncoder().encodeToString(decoded);
            return text.equals(reencoded);
        } catch (IllegalArgumentException e) {
            return false; // 非合法 Base64，不是密文
        }
    }



}
