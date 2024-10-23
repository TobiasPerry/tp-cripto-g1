package steganography;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public sealed interface SteganographyInterface permits LSB1Steganography, LSB4Steganography {
    byte[] DELIMITER = {0, 1, 2, 3, 4}; // Unique byte sequence as delimiter

    /**
     * Encodes a byte array into an image
     * @param coverImagePath Path to the input image
     * @param data Byte array to hide
     * @param outputPath Path for the output image
     * @throws IOException If there's an error handling the image
     */
    void encode(String coverImagePath, byte[] data, String outputPath) throws IOException;

    /**
     * Decodes a byte array from an image
     * @param stegoImagePath Path to the image containing the hidden data
     * @return Decoded byte array or null if no data found
     * @throws IOException If there's an error handling the image
     */
    byte[] decode(String stegoImagePath) throws IOException;

    /**
     * Converts byte array to binary string
     * @param data Byte array to convert
     * @return Binary string representation
     */
    default String bytesToBinary(byte[] data) {
        StringBuilder binary = new StringBuilder();

        // Convert data bytes
        for (byte b : data) {
            binary.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }

        // Add delimiter
        for (byte b : DELIMITER) {
            binary.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }

        return binary.toString();
    }

    /**
     * Converts binary string back to byte array
     * @param binary Binary string to convert
     * @return Byte array of the data
     */
    default byte[] binaryToBytes(String binary) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        StringBuilder currentByte = new StringBuilder();

        for (int i = 0; i < binary.length(); i++) {
            currentByte.append(binary.charAt(i));
            if (currentByte.length() == 8) {
                bytes.write(Integer.parseInt(currentByte.toString(), 2));
                currentByte = new StringBuilder();

                // Check for delimiter
                if (bytes.size() >= DELIMITER.length) {
                    byte[] current = bytes.toByteArray();
                    int start = current.length - DELIMITER.length;
                    byte[] possibleDelimiter = Arrays.copyOfRange(current, start, current.length);
                    if (Arrays.equals(possibleDelimiter, DELIMITER)) {
                        // Return data without delimiter
                        return Arrays.copyOf(current, start);
                    }
                }
            }
        }

        return bytes.toByteArray();
    }
}
