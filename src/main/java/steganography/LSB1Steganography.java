package steganography;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

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

        byte[] extractedData = baos.toByteArray();

        return extractedData;
    }
}
