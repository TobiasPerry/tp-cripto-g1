package steganography;

import utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class LSBISteganography implements SteganographyInterface {

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

        int availableBits = (imageBytes.length - pixelDataOffset - 4) * 8; // Reserve 4 bytes for pattern info

        int totalDataBits = data.length * 8;
        if (totalDataBits > availableBits) {
            throw new IllegalArgumentException("Data too large for cover image");
        }

        byte[] newImage = insertDataLSBI(data, pixelDataOffset, imageBytes);

        // Write the modified image bytes to the output file
        Files.write(new File(outputPath).toPath(), newImage);
    }

    private byte[] insertDataLSBI(byte[] dataToCover, int imageByteOffset, byte[] coverImageBytes) {
        // Map to count changes for each pattern. + 0 for changed, +100 for not changed
        Map<Integer, Integer> changeCountMap = new HashMap<>();

        // Skip first 4 bytes of pixel data (will store pattern information here later)
        int startOffset = imageByteOffset + 4;

        int pixel = 0; // To ensure we skip the red channels

        for (byte b : dataToCover) {
            if (pixel % 3 == 1) {
                pixel++;
                startOffset++;
                continue;
            }

            int pattern = (coverImageBytes[startOffset] >> 1) & 0b11;
            boolean changed = false;

            for (int i = 7; i >= 0; i--) {
                int bit = (b >> i) & 1; // Big-endian order
                if (i == 0) {
                    changed = (byte) ((coverImageBytes[startOffset] & 0xFF)) != (byte) ((coverImageBytes[startOffset] & 0xFE) | bit);
                }
                coverImageBytes[startOffset] = (byte) ((coverImageBytes[startOffset] & 0xFE) | bit);
                startOffset++;
            }

            if (changed) {
                changeCountMap.put(pattern, changeCountMap.getOrDefault(pattern, 0) + 1);
            } else {
                changeCountMap.put(pattern + 100, changeCountMap.getOrDefault(pattern + 100, 0) + 1);
            }
            pixel++;
        }

        // Store inversion state for each pattern in separate bytes
        for (int pattern = 0; pattern < 4; pattern++) {
            int changes = changeCountMap.getOrDefault(pattern, 0);
            int nonChanges = changeCountMap.getOrDefault(pattern + 100, 0);

            // Store 1 if pattern needs inversion (more changes than non-changes)
            boolean inversionState = changes + nonChanges > 1 && changes > nonChanges;
            coverImageBytes[imageByteOffset + pattern] = inversionState ? (byte) ((coverImageBytes[imageByteOffset + pattern] & 0xFE) | 1) : (byte) ((coverImageBytes[imageByteOffset + pattern] & 0xFE) | 0);

            int secondPassOffset = imageByteOffset + 4;
            // If inversion is needed, invert all LSBs for this pattern
            if (inversionState) {
                for (byte b : coverImageBytes) {
                    if (pixel % 3 == 1 || ((coverImageBytes[secondPassOffset] >> 1) & 0b11) != pattern) {
                        pixel++;
                        secondPassOffset++;
                        continue;
                    }

                    byte newByte = (b ^= 1);
                    coverImageBytes[secondPassOffset] = newByte;
                    secondPassOffset += 1;
                    pixel++;
                }
            }
        }

        return coverImageBytes;
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

        // The extracted data
        byte[] extractedData = baos.toByteArray();


        return extractedData;
    }


    // Optional: Store inversion map data for future decoding
    // Encode the map at a specific location or as metadata if necessary.

    @Override
    public String getFileExtension(String stegoImagePath) throws IOException {
        byte[] imageBytes = Files.readAllBytes(new File(stegoImagePath).toPath());

        int pixelDataOffset = ((imageBytes[10] & 0xFF)) |
                ((imageBytes[11] & 0xFF) << 8) |
                ((imageBytes[12] & 0xFF) << 16) |
                ((imageBytes[13] & 0xFF) << 24);

        int imageByteOffset = pixelDataOffset;

        int fileSize = 0;
        for (int i = 0; i < INT_SIZE; i++) {
            int bit = imageBytes[imageByteOffset] & 1;
            fileSize |= (bit << i);
            imageByteOffset++;
        }

        imageByteOffset += fileSize * BITS_IN_BYTE;

        StringBuilder extension = new StringBuilder();
        while (true) {
            int b = 0;
            for (int i = 7; i >= 0; i--) {
                if (imageByteOffset >= imageBytes.length) {
                    throw new IllegalArgumentException("Not enough data in image to extract file extension");
                }
                int bit = imageBytes[imageByteOffset] & 1;
                b |= (bit << i);
                imageByteOffset++;
            }
            if (b == 0) {
                break;
            }
            extension.append((char) b);
        }

        return extension.toString();
    }
}
