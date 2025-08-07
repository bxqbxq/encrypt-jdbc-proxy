package com.seu.jdbcproxy.rewrite;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Decrypt {

    public static String decrypt(String encryptedText) throws Exception {
        //String encryptedText = "REYP2xf05sh1a7kf5eVJCg==";
        String secretKey = "this is fixed test key password.";
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        String plainText = new String(decryptedBytes, StandardCharsets.UTF_8);
        return plainText;
    }

    // 判断数据是否是密文
    public static boolean isCipherText(String text) {
        // 判断是否符合 Base64 编码规则（字母、数字、+/=）
        if (text.matches("^[A-Za-z0-9+/=]+$") && text.length() % 4 == 0) {
            // 进一步尝试解密，成功则认为是密文
            try {
                decrypt(text);
                return true; // 如果能成功解密，认为是密文
            } catch (Exception e) {
                return false; // 解密失败，说明不是密文
            }
        }
        return false;
    }

    // 处理单个数据项，解密密文或直接输出明文
//    public static void processData(String item) throws Exception {
//        //System.out.println("Processing: " + item);
//
//        // 判断数据是否为加密数据
//        if (isCipherText(item)) {
//            // 如果是加密数据，解密并输出
//            String decrypted = decrypt(item);
//            System.out.println("Decrypted: " + decrypted);
//        } else {
//            // 如果是明文，直接输出
//            System.out.println("Plaintext: " + item);
//        }
//        //System.out.println(); // 输出空行，便于区分
//    }

    public static String finalResult(String encryptedText) throws Exception {
        if (isCipherText(encryptedText)) {
            String decrypted = decrypt(encryptedText);
            return decrypted;
        }else {
            return encryptedText;
        }

    }

    public static void main(String[] args) throws Exception {
        // 假设这是一组包含明文和加密数据的字符串
        String[] data = {
                "Hello, this is plain text.",  // 明文
                "qFMuBsORuooyO454P+3gQvUaCkpQvvp2WS6jTUdLI84=",  // 加密数据（Base64编码）
                "Another plain text",  // 明文
                "REYP2xf05sh1a7kf5eVJCg==",  // 加密数据（Base64编码）
                "11/22",  // 日期格式（明文）
                "12345"  // 纯数字（明文）
        };

        for (int i = 0; i < data.length; i++) {
            System.out.println(finalResult(data[i]));
        }

        System.out.println("*****");
        // 在循环里逐个处理每个数据项
//        for (String item : data) {
//            processData(item);
//        }
    }
}


