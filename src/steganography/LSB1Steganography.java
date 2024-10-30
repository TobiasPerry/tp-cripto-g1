package steganography;

import utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LSB1Steganography implements SteganographyInterface {

    private static final int INT_SIZE = 32; // Number of bits in an integer
    private static final int BITS_IN_BYTE = 8;

    @Override
    public void encode(String coverImagePath, String dataPath, String outputPath) throws IOException {
        // Read the BMP image as a byte array
        byte[] imageBytes = Files.readAllBytes(new File(coverImagePath).toPath());
        byte[] data = Files.readAllBytes(Path.of(dataPath));
        String fileExtension = Utils.getFileExtensionFromPath(dataPath);

        // Get the pixel data starting offset from the BMP header (bytes 10 to 13)
        int pixelDataOffset = ((imageBytes[10] & 0xFF)) |
                ((imageBytes[11] & 0xFF) << 8) |
                ((imageBytes[12] & 0xFF) << 16) |
                ((imageBytes[13] & 0xFF) << 24);

        // Calculate the number of bits available for embedding data
        int availableBits = (imageBytes.length - pixelDataOffset) * 8;

        // Prepare the data to hide: file size (4 bytes), actual data, file extension (ending with '\0')
        int dataLength = data.length;

        // Convert data length to 4 bytes (little-endian order)
        byte[] dataLengthBytes = new byte[4];
        dataLengthBytes[0] = (byte)(dataLength & 0xFF);
        dataLengthBytes[1] = (byte)((dataLength >> 8) & 0xFF);
        dataLengthBytes[2] = (byte)((dataLength >> 16) & 0xFF);
        dataLengthBytes[3] = (byte)((dataLength >> 24) & 0xFF);

        byte[] extensionBytes = fileExtension.getBytes();

        // Calculate the total number of bits required for embedding
        int totalDataBits = (dataLengthBytes.length + data.length + extensionBytes.length) * 8;
        // Check if the cover image has enough space
        if (totalDataBits > availableBits) {
            throw new IllegalArgumentException("Data too large for cover image");
        }

        int imageByteOffset = pixelDataOffset;

        // 1. Embed the file size (32 bits)
        for (byte b : dataLengthBytes) {
            for (int i = 0; i < 8; i++) {
                int bit = (b >> i) & 1;
                // Set the LSB of the current image byte
                imageBytes[imageByteOffset] = (byte)((imageBytes[imageByteOffset] & 0xFE) | bit);
                imageByteOffset++;
            }
        }

        // 2. Embed the actual data
        for (byte b : data) {
            for (int i = 7; i >= 0; i--) {
                int bit = (b >> i) & 1; // Big-endian order
                imageBytes[imageByteOffset] = (byte)((imageBytes[imageByteOffset] & 0xFE) | bit);
                imageByteOffset++;
            }
        }

        // 3. Embed the file extension
        for (byte b : extensionBytes) {
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

        // 1. Extract the file size (first 32 bits) in little-endian order
        int fileSize = 0;
        for (int i = 0; i < INT_SIZE; i++) {
            int bit = imageBytes[imageByteOffset] & 1;
            fileSize |= (bit << i); // Little-endian order
            imageByteOffset++;
        }


        System.out.println("Read file size up to" + imageByteOffset);
        // Validate the file size
        if (fileSize <= 0 || fileSize > (imageBytes.length - imageByteOffset) / BITS_IN_BYTE) {
            throw new IllegalArgumentException("Invalid file size extracted");
        }

        byte[] data = new byte[fileSize];

        // 2. Extract the actual data in big-endian order
        for (int i = 0; i < fileSize; i++) {
            int b = 0;
            for (int j = 7; j >= 0; j--) {
                int bit = imageBytes[imageByteOffset] & 1;
                b |= (bit << j); // Big-endian order
                imageByteOffset++;
            }
            data[i] = (byte) b;
        }

        // The data array now contains the hidden file data

        return data;
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
            fileSize |= (bit << i); // Little-endian order
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
