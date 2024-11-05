package steganography;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public final class LSBISteganography implements SteganographyInterface {


    @Override
    public void encode(String coverImagePath, byte[] secretData, String outputPath) throws IOException {
        // Read the cover image
        byte[] imageBytes = Files.readAllBytes(new File(coverImagePath).toPath());

        // Get the pixel data offset from BMP header
        int pixelDataOffset = ((imageBytes[10] & 0xFF)) |
                ((imageBytes[11] & 0xFF) << 8) |
                ((imageBytes[12] & 0xFF) << 16) |
                ((imageBytes[13] & 0xFF) << 24);

        // Create a copy of the image to modify
        byte[] stegoImage = Arrays.copyOf(imageBytes, imageBytes.length);

        // 4 bits for pattern inversion flags
        int availableCapacity = (imageBytes.length - pixelDataOffset - 4) * 2 / 3; // Only Blue and Green channels
        if (secretData.length > availableCapacity) {
            throw new IllegalArgumentException("Secret data is too large for this cover image");
        }

        // First, embed the secret data without pattern analysis
        int startOffset = pixelDataOffset + 4; // Leave 4 bytes for pattern information
        int pixelCounter = 0;
        int secretBitIndex = 0;

        // Arrays to count pattern statistics
        int[][] patternStats = new int[4][2]; // [pattern][changed/unchanged]

        // First pass
        while (secretBitIndex < secretData.length * 8 && startOffset < stegoImage.length) {
            // Skip red channel
            if (pixelCounter % 3 == 1) {
                startOffset++;
                pixelCounter++;
                continue;
            }

            // Get the current bit from secret data
            int byteIndex = secretBitIndex / 8;
            int bitOffset = 7 - (secretBitIndex % 8);
            int secretBit = (secretData[byteIndex] >> bitOffset) & 1;

            // Get the pattern from bits 2-3 (counting from behind)
            int pattern = (stegoImage[startOffset] >> 1) & 0b11;

            // Get original LSB
            int originalLSB = stegoImage[startOffset] & 1;

            // Set the LSB
            stegoImage[startOffset] = (byte) ((stegoImage[startOffset] & 0xFE) | secretBit);

            // Update statistics
            if (originalLSB != secretBit) {
                patternStats[pattern][0]++; // changed
            } else {
                patternStats[pattern][1]++; // unchanged
            }

            secretBitIndex++;
            pixelCounter++;
            startOffset++;
        }

        // Determine which patterns need inversion
        boolean[] patternInversion = new boolean[4];
        for (int i = 0; i < 4; i++) {
            patternInversion[i] = patternStats[i][0] > patternStats[i][1];
        }

        // Second pass: apply pattern inversions
        startOffset = pixelDataOffset + 4;
        pixelCounter = 0;
        secretBitIndex = 0;

        while (secretBitIndex < secretData.length * 8 && startOffset < stegoImage.length) {
            // Skip red channel
            if (pixelCounter % 3 == 1) {
                startOffset++;
                pixelCounter++;
                continue;
            }

            // Get the pattern
            int pattern = (stegoImage[startOffset] >> 1) & 0b11;

            // If this pattern should be inverted, flip the LSB
            if (patternInversion[pattern]) {
                stegoImage[startOffset] ^= 1;
            }

            secretBitIndex++;
            pixelCounter++;
            startOffset++;
        }

        // Store pattern inversion flags in the first 4 bytes of pixel data
        for (int i = 0; i < 4; i++) {
            stegoImage[pixelDataOffset + i] = (byte) ((stegoImage[pixelDataOffset + i] & 0xFE) | (patternInversion[i] ? 1 : 0));
        }

        Files.write(new File(outputPath).toPath(), stegoImage);
    }

    @Override
    public byte[] decode(String stegoImagePath) throws IOException {
        // Read the stego image
        byte[] imageBytes = Files.readAllBytes(new File(stegoImagePath).toPath());

        // Get the pixel data offset from BMP header
        int pixelDataOffset = ((imageBytes[10] & 0xFF)) |
                ((imageBytes[11] & 0xFF) << 8) |
                ((imageBytes[12] & 0xFF) << 16) |
                ((imageBytes[13] & 0xFF) << 24);

        // Read pattern inversion information from the first 4 bytes of pixel data
        boolean[] patternInversion = new boolean[4];
        for (int i = 0; i < 4; i++) {
            patternInversion[i] = (imageBytes[pixelDataOffset + i] & 1) == 1;
        }

        // Skip the 4 bytes used for pattern information
        int startOffset = pixelDataOffset + 4;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int bitCount = 0;
        int currentByte = 0;
        int pixelCounter = 0;

        while (startOffset < imageBytes.length) {

            // If we've read 1 bit from the pixel, skip the second (red channel because I am reading the first 3 bytes' LSBs)
            if (pixelCounter % 3 == 1) {
                startOffset++;
                pixelCounter++;
                continue;
            }

            int bit = imageBytes[startOffset] & 1;

            int bitPattern = (imageBytes[startOffset] >> 1) & 0b11;

            if (patternInversion[bitPattern]) {
                bit ^= 1; // Invert if pattern is marked for inversion
            }

            currentByte = (currentByte << 1) | bit;
            bitCount++;

            if (bitCount == 8) {
                baos.write(currentByte);
                bitCount = 0;
                currentByte = 0;
            }

            pixelCounter++;
            startOffset++;
        }

        byte[] extractedData = baos.toByteArray();

        return extractedData;
    }
}
