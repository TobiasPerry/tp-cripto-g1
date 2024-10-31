package steganography;

import utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LSB4Steganography implements SteganographyInterface {

    private static final int INT_SIZE = 32; // Number of bits in an integer
    private static final int BITS_IN_BYTE = 8;
    private static final int BITS_TO_EMBED = 4; // Number of bits to embed per byte

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
        int availableBits = (imageBytes.length - pixelDataOffset) * BITS_TO_EMBED;

        // Calculate the total number of bits required for embedding
        int totalDataBits = data.length * BITS_IN_BYTE;

        // Check if the cover image has enough space
        if (totalDataBits > availableBits) {
            throw new IllegalArgumentException("Data too large for cover image");
        }

        int imageByteOffset = pixelDataOffset;

        // **Embedding Process**

        // 1. Embed the file size (32 bi        // 2. Embed the actual data
        for (byte b : data) {
            for (int i = 0; i < BITS_IN_BYTE; i += BITS_TO_EMBED) {
                int bits = (b >> (BITS_IN_BYTE - BITS_TO_EMBED - i)) & 0x0F;
                imageBytes[imageByteOffset] = (byte) ((imageBytes[imageByteOffset] & 0xF0) | bits);
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
            // Extract the 4 LSBs from the current image byte
            int bits = imageBytes[imageByteOffset] & 0x0F;
            currentByte = (currentByte << BITS_TO_EMBED) | bits;
            bitCount += BITS_TO_EMBED;

            while (bitCount >= 8) {
                // Extract the top 8 bits from currentByte
                int byteToWrite = (currentByte >> (bitCount - BITS_IN_BYTE)) & 0xFF;
                baos.write(byteToWrite);
                bitCount -= 8;
                // Keep the remaining bits in currentByte
                currentByte = currentByte & ((1 << bitCount) - 1);
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
        byte[] imageBytes = Files.readAllBytes(Path.of(stegoImagePath));

        // Get the pixel data starting offset from the BMP header (bytes 10 to 13)
        int pixelDataOffset = ((imageBytes[10] & 0xFF)) |
                ((imageBytes[11] & 0xFF) << 8) |
                ((imageBytes[12] & 0xFF) << 16) |
                ((imageBytes[13] & 0xFF) << 24);

        int imageByteOffset = pixelDataOffset;

        // **Extraction Process**

        // 1. Extract the file size (32 bits)
        byte[] dataLengthBytes = new byte[4];
        for (int b = 0; b < dataLengthBytes.length; b++) {
            int combinedByte = 0;
            for (int i = 0; i < BITS_IN_BYTE; i += BITS_TO_EMBED) {
                int bits = imageBytes[imageByteOffset] & 0x0F; // Get the lower 4 bits
                combinedByte |= (bits << i);
                imageByteOffset++;
            }
            dataLengthBytes[b] = (byte) combinedByte;
        }

        // Convert the byte array to an integer (big-endian order)
        int dataLength = ((dataLengthBytes[0] & 0xFF) << 24) |
                ((dataLengthBytes[1] & 0xFF) << 16) |
                ((dataLengthBytes[2] & 0xFF) << 8) |
                (dataLengthBytes[3] & 0xFF);

        // 2. Skip over the embedded data
        int dataBytesToSkip = dataLength * (BITS_IN_BYTE / BITS_TO_EMBED);
        imageByteOffset += dataBytesToSkip;

        // 3. Extract the file extension
        ByteArrayOutputStream extensionStream = new ByteArrayOutputStream();
        boolean nullTerminatorFound = false;

        while (!nullTerminatorFound) {
            int combinedByte = 0;
            for (int i = BITS_IN_BYTE - BITS_TO_EMBED; i >= 0; i -= BITS_TO_EMBED) {
                int bits = imageBytes[imageByteOffset] & 0x0F;
                combinedByte |= (bits << i);
                imageByteOffset++;
            }
            if (combinedByte == 0) {
                nullTerminatorFound = true;
            } else {
                extensionStream.write(combinedByte);
            }
        }

        // Convert the extension bytes to a string
        String fileExtension = new String(extensionStream.toByteArray(), StandardCharsets.UTF_8);

        return fileExtension;
    }
}
