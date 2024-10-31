package steganography;

import utils.Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LSB1Steganography implements SteganographyInterface {

    private static final int INT_SIZE = 32; // Number of bits in an integer
    private static final int BITS_IN_BYTE = 8;

    @Override
    public void encode(String coverImagePath, byte[] data, String outputPath) throws IOException {
        // Read the BMP image as a byte array
        byte[] imageBytes = Files.readAllBytes(new File(coverImagePath).toPath());

        // Get the pixel data starting offset from the BMP header (bytes 10 to 13)
        int pixelDataOffset = ((imageBytes[10] & 0xFF)) |
                ((imageBytes[11] & 0xFF) << 8) |
                ((imageBytes[12] & 0xFF) << 16) |
                ((imageBytes[13] & 0xFF) << 24);

        // Calculate the number of bits available for embedding data
        int availableBits = (imageBytes.length - pixelDataOffset) * 8;

        // Calculate the total number of bits required for embedding
        int totalDataBits = data.length * 8;
        // Check if the cover image has enough space
        if (totalDataBits > availableBits) {
            throw new IllegalArgumentException("Data too large for cover image");
        }

        int imageByteOffset = pixelDataOffset;

        //Embed the actual data
        for (byte b : data) {
            for (int i = 7; i >= 0; i--) {
                int bit = (b >> i) & 1; // Big-endian order
                imageBytes[imageByteOffset] = (byte)((imageBytes[imageByteOffset] & 0xFE) | bit);
                imageByteOffset++;
            }
        }

        // Write the modified image bytes to the output file
        Files.write(new File(outputPath).toPath(), imageBytes);
    }

    @Override
    public byte[] decode(String stegoImagePath) throws IOException {
        // Read the stego image as a byte array
        byte[] imageBytes = Files.readAllBytes(new File(stegoImagePath).toPath());

        // Get the pixel data starting offset
        int pixelDataOffset = ((imageBytes[10] & 0xFF)) |
                ((imageBytes[11] & 0xFF) << 8) |
                ((imageBytes[12] & 0xFF) << 16) |
                ((imageBytes[13] & 0xFF) << 24);

        int imageByteOffset = pixelDataOffset;

        // **Decoding Process**

        // Extract the embedded data
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int bitCount = 0;
        int currentByte = 0;

        while (imageByteOffset < imageBytes.length) {
            int bit = imageBytes[imageByteOffset] & 1;
            currentByte = (currentByte << 1) | bit;
            bitCount++;

            if (bitCount == 8) {
                baos.write(currentByte);
                bitCount = 0;
                currentByte = 0;
            }

            imageByteOffset++;
        }

        // The extracted data
        byte[] extractedData = baos.toByteArray();

        return extractedData;
    }



    @Override
    public String getFileExtension(String stegoImagePath) throws IOException {
        // Read the stego image as a byte array
        byte[] imageBytes = Files.readAllBytes(new File(stegoImagePath).toPath());

        // Get the pixel data starting offset
        int pixelDataOffset = ((imageBytes[10] & 0xFF)) |
                ((imageBytes[11] & 0xFF) << 8) |
                ((imageBytes[12] & 0xFF) << 16) |
                ((imageBytes[13] & 0xFF) << 24);

        int imageByteOffset = pixelDataOffset;

        // **Extension Extraction Process**

        // 1. Extract the file size (first 32 bits) in little-endian order
        int fileSize = 0;
        for (int i = 0; i < INT_SIZE; i++) {
            int bit = imageBytes[imageByteOffset] & 1;
            fileSize |= (bit << (INT_SIZE - i - 1));
            imageByteOffset++;
        }

        // Skip over the data bits
        imageByteOffset += fileSize * BITS_IN_BYTE;

        // 4. Extract the file extension character by character until the null terminator in big-endian order
        StringBuilder extension = new StringBuilder();
        while (true) {
            int b = 0;
            for (int i = 7; i >= 0; i--) {
                if (imageByteOffset >= imageBytes.length) {
                    throw new IllegalArgumentException("Not enough data in image to extract file extension");
                }
                int bit = imageBytes[imageByteOffset] & 1;
                b |= (bit << i); // Big-endian order
                imageByteOffset++;
            }
            if (b == 0) {
                break; // Null terminator found
            }
            extension.append((char) b);
        }
        System.out.println(extension.toString());

        return extension.toString();
    }
}
