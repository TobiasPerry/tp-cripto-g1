package steganography;

import utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LSB4Steganography implements SteganographyInterface {

    private static final int INT_SIZE = 32; // Number of bits in an integer
    private static final int BITS_IN_BYTE = 8;
    private static final int BITS_TO_EMBED = 4; // Number of bits to embed per byte

    @Override
    public void encode(String coverImagePath, String dataPath, String outputPath) throws IOException {
        // Read the BMP image as a byte array
        byte[] imageBytes = Files.readAllBytes(new File(coverImagePath).toPath());
        byte[] data = Files.readAllBytes(Path.of(dataPath));
        String fileExtension = Utils.getFileExtensionFromPath(dataPath) + "\0"; // Add null terminator

        // Get the pixel data starting offset from the BMP header (bytes 10 to 13)
        int pixelDataOffset = ((imageBytes[10] & 0xFF)) |
                ((imageBytes[11] & 0xFF) << 8) |
                ((imageBytes[12] & 0xFF) << 16) |
                ((imageBytes[13] & 0xFF) << 24);

        // Calculate the number of bits available for embedding data
        int availableBits = (imageBytes.length - pixelDataOffset) * BITS_TO_EMBED;

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
        int totalDataBits = (dataLengthBytes.length + data.length + extensionBytes.length) * BITS_IN_BYTE;

        // Check if the cover image has enough space
        if (totalDataBits > availableBits) {
            throw new IllegalArgumentException("Data too large for cover image");
        }

        int imageByteOffset = pixelDataOffset;

        // **Embedding Process**

        // 1. Embed the file size (32 bits)
        for (byte b : dataLengthBytes) {
            for (int i = 0; i < BITS_IN_BYTE; i += BITS_TO_EMBED) {
                int bits = (b >> i) & 0x0F; // Get 4 bits
                // Set the 4 LSBs of the current image byte
                imageBytes[imageByteOffset] = (byte)((imageBytes[imageByteOffset] & 0xF0) | bits);
                imageByteOffset++;
            }
        }

        // 2. Embed the actual data
        for (byte b : data) {
            for (int i = 0; i < BITS_IN_BYTE; i += BITS_TO_EMBED) {
                int bits = (b >> (BITS_IN_BYTE - BITS_TO_EMBED - i)) & 0x0F; // Big-endian order
                imageBytes[imageByteOffset] = (byte)((imageBytes[imageByteOffset] & 0xF0) | bits);
                imageByteOffset++;
            }
        }

        // 3. Embed the file extension
        for (byte b : extensionBytes) {
            for (int i = 0; i < BITS_IN_BYTE; i += BITS_TO_EMBED) {
                int bits = (b >> (BITS_IN_BYTE - BITS_TO_EMBED - i)) & 0x0F; // Big-endian order
                imageBytes[imageByteOffset] = (byte)((imageBytes[imageByteOffset] & 0xF0) | bits);
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

        // 1. Extract the file size (first 32 bits)
        int fileSize = 0;
        for (int i = 0; i < 4; i++) {
            int b = 0;
            for (int j = 0; j < BITS_IN_BYTE; j += BITS_TO_EMBED) {
                int bits = imageBytes[imageByteOffset] & 0x0F;
                b |= (bits << j);
                imageByteOffset++;
            }
            fileSize |= (b << (i * 8)); // Little-endian order
        }

        // Validate the file size
        if (fileSize <= 0 || fileSize > (imageBytes.length - imageByteOffset) * BITS_TO_EMBED / BITS_IN_BYTE) {
            throw new IllegalArgumentException("Invalid file size extracted");
        }

        byte[] data = new byte[fileSize];

        // 2. Extract the actual data
        for (int i = 0; i < fileSize; i++) {
            int b = 0;
            for (int j = 0; j < BITS_IN_BYTE; j += BITS_TO_EMBED) {
                int bits = imageBytes[imageByteOffset] & 0x0F;
                b = (b << BITS_TO_EMBED) | bits; // Big-endian order
                imageByteOffset++;
            }
            data[i] = (byte) b;
        }

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

        // 1. Extract the file size (first 32 bits)
        int fileSize = 0;
        for (int i = 0; i < 4; i++) {
            int b = 0;
            for (int j = 0; j < BITS_IN_BYTE; j += BITS_TO_EMBED) {
                int bits = imageBytes[imageByteOffset] & 0x0F;
                b |= (bits << j);
                imageByteOffset++;
            }
            fileSize |= (b << (i * 8)); // Little-endian order
        }

        // Skip over the data bits
        imageByteOffset += (fileSize * BITS_IN_BYTE) / BITS_TO_EMBED;

        // 2. Extract the file extension
        StringBuilder extension = new StringBuilder();
        while (true) {
            int b = 0;
            for (int j = 0; j < BITS_IN_BYTE; j += BITS_TO_EMBED) {
                if (imageByteOffset >= imageBytes.length) {
                    throw new IllegalArgumentException("Not enough data in image to extract file extension");
                }
                int bits = imageBytes[imageByteOffset] & 0x0F;
                b = (b << BITS_TO_EMBED) | bits; // Big-endian order
                imageByteOffset++;
            }
            if (b == 0) {
                break; // Null terminator found
            }
            extension.append((char) b);
        }

        return extension.toString();
    }
}
