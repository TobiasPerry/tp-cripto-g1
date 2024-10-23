package steganography;

import java.io.File;
import java.io.IOException;

public interface SteganographyInterface {
    String DELIMITER = "#####";

    /**
     * Encodes a message into an image
     * @param imagePath Path to the input image
     * @param message Message to encode
     * @param outputPath Path for the output image
     * @throws IOException If there's an error handling the image
     */
    void encode(String imagePath, String message, String outputPath) throws IOException;

    /**
     * Decodes a message from an image
     * @param imagePath Path to the image containing the hidden message
     * @return The decoded message, or null if no message is found
     * @throws IOException If there's an error handling the image
     */
    String decode(String imagePath) throws IOException;

}
