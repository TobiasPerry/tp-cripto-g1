package steganography;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public sealed interface SteganographyInterface permits LSB1Steganography, LSB4Steganography, LSBISteganography {
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

}
