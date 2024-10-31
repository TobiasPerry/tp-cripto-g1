package steganography;

import utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class LSBISteganography implements SteganographyInterface {

    private static final int INT_SIZE = 32; // Number of bits in an integer
    private static final int BITS_IN_BYTE = 8;

    private static final Map<Integer, Boolean> changedPatterns = new HashMap<>();

    @Override
    public void encode(String coverImagePath, byte[] data, String outputPath) throws IOException {
        // Read the BMP image as a byte array
        byte[] imageBytes = Files.readAllBytes(new File(coverImagePath).toPath());

        // Get the pixel data starting offset from the BMP header (bytes 10 to 13)
        int pixelDataOffset = ((imageBytes[10] & 0xFF)) |
                ((imageBytes[11] & 0xFF) << 8) |
                ((imageBytes[12] & 0xFF) << 16) |
                ((imageBytes[13] & 0xFF) << 24);

        int availableBits = (imageBytes.length - pixelDataOffset) * 8;

        int totalDataBits = data.length * 8;
        if (totalDataBits > availableBits) {
            throw new IllegalArgumentException("Data too large for cover image");
        }

        byte[] newImage = insertDataLSBI(data, pixelDataOffset,imageBytes);

        // Write the modified image bytes to the output file
        Files.write(new File(outputPath).toPath(), newImage);
    }

    private byte[] insertDataLSBI(byte[] dataToCover, int imageByteOffset, byte[] coverImageBytes) {
        // Map to count changes for each pattern. + 0 for changed, +100 for not changed
        Map<Integer, Integer> changeCountMap = new HashMap<>();
        int messageIndex = 0;  // Index for tracking position in the secretMessage array
        int bitIndex = 0; // Bit position within the current byte of the message

        // Embedding the secret message
        for (int i = imageByteOffset; i < coverImageBytes.length - imageByteOffset && messageIndex < dataToCover.length; i += 3) {
            // Extract blue and green channels from the pixel (assuming BGR format)
            int blue = coverImageBytes[i] & 0xFF;    // Blue channel
            int green = coverImageBytes[i + 1] & 0xFF; // Green channel

            // Check the pattern of the 2nd and 3rd bits in each channel (00, 01, 10, or 11)
            int bluePattern = (blue >> 1) & 0b11;
            int greenPattern = (green >> 1) & 0b11;

            // Initialize the change count for the current patterns
            changeCountMap.put(bluePattern, changeCountMap.getOrDefault(bluePattern, 0));
            changeCountMap.put(greenPattern, changeCountMap.getOrDefault(greenPattern, 0));
            changeCountMap.put(bluePattern + 100, changeCountMap.getOrDefault(bluePattern + 100, 0));
            changeCountMap.put(greenPattern + 100, changeCountMap.getOrDefault(greenPattern + 100, 0));

            // Embed two bits (one in each channel) per pixel
            for (int j = 0; j < 2 && messageIndex < dataToCover.length; j++) {
                int bitToEmbed = (dataToCover[messageIndex] >> bitIndex) & 1;

                if (j == 0) {
                    // Embed the bit in the green channel
                    if (green != ((green & ~1) | bitToEmbed)) {
                        changeCountMap.put(greenPattern, changeCountMap.getOrDefault(greenPattern , 0) + 1);
                    }
                    else {
                        changeCountMap.put(greenPattern + 100, changeCountMap.getOrDefault(greenPattern+100, 0) + 1);
                    }
                    green = (green & ~1) | bitToEmbed;  // Set LSB to bitToEmbed
                } else {
                    // Embed the bit in the blue channel
                    if (blue != ((blue & ~1) | bitToEmbed)) {
                        changeCountMap.put(bluePattern, changeCountMap.getOrDefault(bluePattern, 0) + 1);
                    }
                    else {
                        changeCountMap.put(bluePattern+100, changeCountMap.getOrDefault(bluePattern+100, 0) + 1);
                    }
                    blue = (blue & ~1) | bitToEmbed;    // Set LSB to bitToEmbed
                }

                // Move to the next bit
                bitIndex++;
                if (bitIndex == 8) {
                    bitIndex = 0;
                    messageIndex++;  // Move to the next byte in secretMessage
                }
            }

            // Replace values in the cover image
            coverImageBytes[i] = (byte) blue;    // Update blue channel
//            System.out.println(blue);
            coverImageBytes[i + 1] = (byte) green; // Update green channel
//            System.out.println(green);

//            // Store the new patterns
//            int newGreenPattern = (green >> 1) & 0b11;
//            int newBluePattern = (blue >> 1) & 0b11;
//
//            // Count changes if patterns have changed. SHOULD NOT HAPPEN.
//            if (greenPattern != newGreenPattern) {
//                System.out.println("Green pattern changed. Wrong.");
//            }
//            if (bluePattern != newBluePattern) {
//                System.out.println("Blue pattern changed. Wrong.");
//            }
        }

        // Second pass: Optimize patterns to reduce changes in the image
        for (Map.Entry<Integer, Integer> entry : changeCountMap.entrySet()) {
            int pattern = entry.getKey();

            if (pattern >= 100) {
                continue; // Skip pattern counts of not changed
            }

            int changes = entry.getValue();
            int nonChanges = changeCountMap.get(pattern + 100);

            // Decide whether to invert LSB based on the number of changes for the pattern
            if (changes + nonChanges > 1 && changes > nonChanges) { // If there were more changes than non-changes, we invert
                for (int i = imageByteOffset; i < coverImageBytes.length - imageByteOffset; i += 3) {
                    int blue = coverImageBytes[i] & 0xFF;    // Blue channel
                    int green = coverImageBytes[i + 1] & 0xFF; // Green channel

                    // Check if the current pixel matches the pattern
                    if (((blue >> 1) & 0b11) == pattern){
                        blue ^= 1;
                    }
                    if (((green >> 1) & 0b11) == pattern) {
                        green ^= 1;
                    }

                    // Update the cover image
                    coverImageBytes[i] = (byte) blue;      // Update blue channel
                    coverImageBytes[i + 1] = (byte) green; // Update green channel
                }
            }
        }
        return coverImageBytes;
    }

        // Optional: Store inversion map data for future decoding
        // Encode the map at a specific location or as metadata if necessary.


    @Override
    public byte[] decode(String stegoImagePath) throws IOException {
        byte[] imageBytes = Files.readAllBytes(new File(stegoImagePath).toPath());

        int pixelDataOffset = ((imageBytes[10] & 0xFF)) |
                ((imageBytes[11] & 0xFF) << 8) |
                ((imageBytes[12] & 0xFF) << 16) |
                ((imageBytes[13] & 0xFF) << 24);

        int imageByteOffset = pixelDataOffset;
        int fileSize = 0;

        // Extract the file size (first 32 bits) in little-endian order DEBERIA SER BIG ENDIAN
        for (int i = 0; i < INT_SIZE; i++) {
            int bit = imageBytes[imageByteOffset] & 1;
            fileSize |= (bit << i);
            imageByteOffset++;
        }

        if (fileSize <= 0 || fileSize > (imageBytes.length - imageByteOffset) / BITS_IN_BYTE) {
            throw new IllegalArgumentException("Invalid file size extracted");
        }

        byte[] data = new byte[fileSize];

        // Extract the actual data in big-endian order
        for (int i = 0; i < fileSize; i++) {
            int b = 0;
            for (int j = 7; j >= 0; j--) {
                int bit = imageBytes[imageByteOffset] & 1;
                b |= (bit << j);
                imageByteOffset++;
            }
            data[i] = (byte) b;
        }

        return data;
    }

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
