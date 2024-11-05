package cryptography;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Security;
import java.security.spec.KeySpec;
import java.util.Arrays;

public class Crypto {

    private final String algorithm;
    private final String mode;
    private final String password;
    private final byte[] salt;
    private int keyLength;
    private int ivLength;
    private String transformation;

    public Crypto(String algorithm, String mode, String password) {
        this.algorithm = algorithm.toLowerCase();
        this.mode = mode.toUpperCase();
        this.password = password;
        this.salt = new byte[8];
        Arrays.fill(salt, (byte) 0x00);

        configureAlgorithm();
    }

    private void configureAlgorithm() {
        switch (algorithm) {
            case "aes128" -> {
                keyLength = 128;
                ivLength = 16;
            }
            case "aes192" -> {
                keyLength = 192;
                ivLength = 16;
            }
            case "aes256" -> {
                keyLength = 256;
                ivLength = 16;
            }
            case "3des" -> {
                keyLength = 192;
                ivLength = 8;
            }
            default -> throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }

        String padding = (mode.startsWith("CFB") || mode.startsWith("OFB")) ? "NoPadding" : "PKCS5Padding";


        transformation = switch (algorithm) {
            case "aes128", "aes192", "aes256" -> String.format("AES/%s/%s", mode, padding);
            case "3des" -> String.format("DESede/%s/%s", mode, padding);
            default -> throw new IllegalArgumentException("Unsupported transformation configuration");
        };
    }

    private SecretKey generateSecretKey(byte[] keyBytes) throws Exception {
        return switch (algorithm) {
            case "aes128", "aes192", "aes256" -> new SecretKeySpec(keyBytes, "AES");
            case "3des" -> new SecretKeySpec(keyBytes, "DESede");
            default -> throw new IllegalArgumentException("Unsupported algorithm");
        };
    }

    private byte[] deriveKeyAndIv() throws Exception {

        int totalKeyLength = keyLength + (mode.equals("ECB") ? 0 : ivLength * 8); 
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, totalKeyLength);
        SecretKey tmp = factory.generateSecret(spec);
        return tmp.getEncoded();
    }

    public byte[] encryptData(byte[] data) throws Exception {
        byte[] keyAndIV = deriveKeyAndIv();
        byte[] keyBytes = Arrays.copyOfRange(keyAndIV, 0, keyLength / 8);
        byte[] ivBytes = mode.equals("ECB") ? new byte[0] : Arrays.copyOfRange(keyAndIV, keyLength / 8, (keyLength + ivLength * 8) / 8);

        SecretKey key = generateSecretKey(keyBytes);
        Cipher cipher = Cipher.getInstance(transformation);

        if ("ECB".equalsIgnoreCase(mode)) {
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } else {
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        }

        return cipher.doFinal(data);
    }

    public byte[] decryptData(byte[] data) throws Exception {
        byte[] keyAndIV = deriveKeyAndIv();
        byte[] keyBytes = Arrays.copyOfRange(keyAndIV, 0, keyLength / 8);
        byte[] ivBytes = mode.equals("ECB") ? new byte[0] : Arrays.copyOfRange(keyAndIV, keyLength / 8, (keyLength + ivLength * 8) / 8);

        SecretKey key = generateSecretKey(keyBytes);
        Cipher cipher = Cipher.getInstance(transformation);

        if ("ECB".equalsIgnoreCase(mode)) {
            cipher.init(Cipher.DECRYPT_MODE, key);
        } else {
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        }

        return cipher.doFinal(data);
    }
}