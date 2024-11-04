import cryptography.Crypto;
import steganography.LSB1Steganography;
import steganography.LSB4Steganography;
import steganography.LSBISteganography;
import steganography.SteganographyInterface;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    private static final String EMBED = "-embed";
    private static final String EXTRACT = "-extract";
    private static final String IN = "-in";
    private static final String P = "-p";
    private static final String OUT = "-out";
    private static final String STEG = "-steg";
    private static final String A = "-a";
    private static final String M = "-m";
    private static final String PASS = "-pass";

    private static String in;
    private static String p;
    private static String out;
    private static String steg;
    private static String a;
    private static String m;
    private static String pass;
    private static boolean embed = false;

    public static void main(String[] args) throws Exception {
        // Process command-line arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case EMBED:
                    embed = true;
                    break;
                case EXTRACT:
                    break;
                case IN:
                    in = args[i + 1];
                    break;
                case P:
                    p = args[i + 1];
                    break;
                case OUT:
                    out = args[i + 1];
                    break;
                case STEG:
                    steg = args[i + 1];
                    break;
                case A:
                    a = args[i + 1];
                    break;
                case M:
                    m = args[i + 1];
                    break;
                case PASS:
                    pass = args[i + 1];
                    break;
            }
        }
        verifyArgs();
        if (embed) {
            embed(in, p, out, steg, a, m, pass);
        } else {
            extract(p, out, steg, a, m, pass);
        }
    }

    private static SteganographyInterface getSteg(String steg) {
        switch (steg) {
            case "LSB1":
                return new LSB1Steganography();
            case "LSB4":
                return new LSB4Steganography();
            case "LSBI":
                return new LSBISteganography();
        }
        throw new IllegalArgumentException("Not a valid Steganography method");
    }

    private static void embed(String in, String p, String out, String steg, String a, String m, String pass) throws Exception {
        SteganographyInterface lsb = getSteg(steg);

        // Read the file to hide
        byte[] fileData = Files.readAllBytes(Path.of(in));
        int realSize = fileData.length;
        String extension = getFileExtension(in);

        // Build the sequence: real size || file data || extension
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Write real size (4 bytes, Big Endian)
        dos.writeInt(realSize);
        // Write file data
        dos.write(fileData);
        // Write extension with null terminator
        String extWithDot = extension + '\0';
        dos.write(extWithDot.getBytes("UTF-8"));

        dos.close();
        byte[] dataToEncrypt = baos.toByteArray();

        byte[] dataToHide;

        if (a == null || m == null || pass == null) {
            // No encryption: Just use the data as is
            dataToHide = dataToEncrypt;
        } else {
            // Encrypt data using Cryptography class
            Crypto crypto = new Crypto(a, m, pass);
            byte[] encryptedData = crypto.encryptData(dataToEncrypt);

            // Build the sequence: ciphertext size || encrypted data
            ByteArrayOutputStream baosSteg = new ByteArrayOutputStream();
            DataOutputStream dosSteg = new DataOutputStream(baosSteg);
            dosSteg.writeInt(encryptedData.length); // Ciphertext size in Big-Endian
            dosSteg.write(encryptedData);           // Encrypted data

            dosSteg.close();
            dataToHide = baosSteg.toByteArray();
        }

        // Steganograph the data
        lsb.encode(p, dataToHide, out);
    }

    private static void extract(String p, String out, String steg, String a, String m, String pass) throws Exception {
        SteganographyInterface lsb = getSteg(steg);
        byte[] stegoData = lsb.decode(p);

        byte[] decryptedData;

        if (a == null || m == null || pass == null) { // No decryption
            // Data is in the format: realSize || fileData || extension
            decryptedData = stegoData;
        } else {
            // Data is in the format: ciphertextSize (4 bytes) || encryptedData
            ByteArrayInputStream bais = new ByteArrayInputStream(stegoData);
            DataInputStream dis = new DataInputStream(bais);

            // Read the ciphertext size
            int ciphertextSize = dis.readInt();

            // Read the encrypted data
            byte[] encryptedData = new byte[ciphertextSize];
            dis.readFully(encryptedData);

            // Decrypt data using Cryptography class
            Crypto crypto = new Crypto(a, m, pass);
            decryptedData = crypto.decryptData(encryptedData);
        }

        // Now, decryptedData contains: realSize || fileData || extension

        // Extract realSize, fileData, and extension from decryptedData
        ByteArrayInputStream dataBais = new ByteArrayInputStream(decryptedData);
        DataInputStream dataDis = new DataInputStream(dataBais);

        int realSize = dataDis.readInt();

        if (realSize + 4 > decryptedData.length) {
            throw new IllegalArgumentException("Real size is greater than the data size");
        }

        // Read the file data
        byte[] fileData = new byte[realSize];
        dataDis.readFully(fileData);

        // Read the extension until '\0'
        ByteArrayOutputStream extBaos = new ByteArrayOutputStream();
        int b;
        while ((b = dataDis.read()) != -1 && b != 0) {
            extBaos.write(b);
        }
        String extension = extBaos.toString("UTF-8");

        // Save the extracted file
        saveFile(fileData, out, extension);
    }


    private static void verifyArgs() {
        if (embed) {
            if (in == null || p == null || out == null || steg == null) {
                throw new IllegalArgumentException("Missing arguments");
            }
        } else {
            if (p == null || out == null || steg == null) {
                throw new IllegalArgumentException("Missing arguments");
            }
        }
    }

    private static void saveFile(byte[] bytes, String fileName, String fileExtension) throws IOException {
        String filePath = fileName + fileExtension;
        File file = new File(filePath);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
        }
    }

    private static String getFileExtension(String filename) {
        String ext = "";
        int i = filename.lastIndexOf('.');
        if (i > 0) {
            ext = filename.substring(i);
        }
        return ext;
    }
}
