package cryptography;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
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

        // Define padding type based on mode
        String padding = (mode.equals("CFB") || mode.equals("OFB")) ? "NoPadding" : "PKCS5Padding";

        transformation = switch (algorithm) {
            case "aes128", "aes192", "aes256" -> String.format("AES/%s/%s", mode, padding);
            case "des" -> String.format("DES/%s/%s", mode, padding);
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

    private byte[] deriveKey() throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, keyLength);
        SecretKey tmp = factory.generateSecret(spec);
        return tmp.getEncoded();
    }

    private byte[] deriveIv() throws Exception {
        if (mode.equals("ECB")) {
            return new byte[0]; // No IV needed for ECB mode
        }
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec((password + "IV").toCharArray(), salt, 10000, ivLength * 8);
        SecretKey tmp = factory.generateSecret(spec);
        return tmp.getEncoded();
    }

    public byte[] encryptData(byte[] data) throws Exception {
        byte[] keyBytes = deriveKey();
        byte[] ivBytes = deriveIv();

        SecretKey key = generateSecretKey(keyBytes);
        Cipher cipher = Cipher.getInstance(transformation);

        // Initialize cipher based on mode
        if ("ECB".equalsIgnoreCase(mode)) {
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } else {
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        }

        return cipher.doFinal(data);
    }

    public byte[] decryptData(byte[] data) throws Exception {
        byte[] keyBytes = deriveKey();
        byte[] ivBytes = deriveIv();

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
