package cryptography;

import java.security.spec.KeySpec;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.*;

public class Crypto {

    private String algorithm;
    private String mode;
    private String password;
    private byte[] salt;
    private int keyLength;
    private int ivLength;
    private String transformation;

    public Crypto(String algorithm, String mode, String password) {
        this.algorithm = algorithm;
        this.mode = mode;
        this.password = password;
        this.salt = new byte[8];
        Arrays.fill(salt, (byte) 0x00);

        configureAlgorithm();
    }

    private void configureAlgorithm() {
        switch (algorithm) {
            case "aes128":
                keyLength = 128;
                ivLength = 128;
                break;
            case "aes192":
                keyLength = 192;
                ivLength = 128;
                break;
            case "aes256":
                keyLength = 256;
                ivLength = 128;
                break;
            case "des":
                keyLength = 64;
                ivLength = 64;
                break;
            case "3des":
                keyLength = 192;
                ivLength = 64;
                break;
            default:
                throw new IllegalArgumentException("Unsupported algorithm");
        }

        switch (algorithm) {
            case "aes128":
            case "aes192":
            case "aes256":
                switch (mode) {
                    case "ecb":
                        transformation = "AES/ECB/PKCS5Padding";
                        break;
                    case "cbc":
                        transformation = "AES/CBC/PKCS5Padding";
                        break;
                    case "cfb":
                        transformation = "AES/CFB/NoPadding";
                        break;
                    case "ofb":
                        transformation = "AES/OFB/NoPadding";
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported mode");
                }
                break;
            case "des":
                switch (mode) {
                    case "ecb":
                        transformation = "DES/ECB/PKCS5Padding";
                        break;
                    case "cbc":
                        transformation = "DES/CBC/PKCS5Padding";
                        break;
                    case "cfb":
                        transformation = "DES/CFB/NoPadding";
                        break;
                    case "ofb":
                        transformation = "DES/OFB/NoPadding";
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported mode");
                }
                break;
            case "3des":
                switch (mode) {
                    case "ecb":
                        transformation = "DESede/ECB/PKCS5Padding";
                        break;
                    case "cbc":
                        transformation = "DESede/CBC/PKCS5Padding";
                        break;
                    case "cfb":
                        transformation = "DESede/CFB/NoPadding";
                        break;
                    case "ofb":
                        transformation = "DESede/OFB/NoPadding";
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported mode");
                }
                break;
        }
    }

    public byte[] encryptData(byte[] data) throws Exception {
        int totalKeyLength = keyLength + ivLength;

        // Generate key and IV using PBKDF2
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 1000, totalKeyLength);
        SecretKey tmp = factory.generateSecret(spec);
        byte[] keyAndIV = tmp.getEncoded();

        // Separate key and IV
        byte[] keyBytes = Arrays.copyOfRange(keyAndIV, 0, keyLength / 8);
        byte[] ivBytes = Arrays.copyOfRange(keyAndIV, keyLength / 8, totalKeyLength / 8);

        SecretKey key = null;
        switch (algorithm) {
            case "aes128":
            case "aes192":
            case "aes256":
                key = new SecretKeySpec(keyBytes, "AES");
                break;
            case "des":
                key = new SecretKeySpec(keyBytes, "DES");
                break;
            case "3des":
                key = new SecretKeySpec(keyBytes, "DESede");
                break;
        }

        Cipher cipher = Cipher.getInstance(transformation);

        if (mode.equals("ecb")) {
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } else {
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        }

        return cipher.doFinal(data);
    }

    public byte[] decryptData(byte[] data) throws Exception {
        int totalKeyLength = keyLength + ivLength;

        // Generate key and IV using PBKDF2
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 1000, totalKeyLength);
        SecretKey tmp = factory.generateSecret(spec);
        byte[] keyAndIV = tmp.getEncoded();

        // Separate key and IV
        byte[] keyBytes = Arrays.copyOfRange(keyAndIV, 0, keyLength / 8);
        byte[] ivBytes = Arrays.copyOfRange(keyAndIV, keyLength / 8, totalKeyLength / 8);

        SecretKey key = null;
        switch (algorithm) {
            case "aes128":
            case "aes192":
            case "aes256":
                key = new SecretKeySpec(keyBytes, "AES");
                break;
            case "des":
                key = new SecretKeySpec(keyBytes, "DES");
                break;
            case "3des":
                key = new SecretKeySpec(keyBytes, "DESede");
                break;
        }

        Cipher cipher = Cipher.getInstance(transformation);

        if (mode.equals("ecb")) {
            cipher.init(Cipher.DECRYPT_MODE, key);
        } else {
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        }

        return cipher.doFinal(data);
    }
}
