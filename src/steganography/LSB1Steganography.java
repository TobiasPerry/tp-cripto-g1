package steganography;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class LSB1Steganography implements SteganographyInterface {
    @Override
    public void encode(String coverImagePath, byte[] data, String outputPath) throws IOException {
        BufferedImage image = ImageIO.read(new File(coverImagePath));
        String binaryData = bytesToBinary(data);

        // Each pixel can store 3 bits (1 in each RGB channel)
        if (binaryData.length() > image.getWidth() * image.getHeight() * 3) {
            throw new IllegalArgumentException("Data too large for cover image");
        }

        int messageIndex = 0;
        // Start from bottom row, move up
        for (int y = image.getHeight() - 1; y >= 0 && messageIndex < binaryData.length(); y--) {
            // Move left to right
            for (int x = 0; x < image.getWidth() && messageIndex < binaryData.length(); x++) {
                int pixel = image.getRGB(x, y);

                // Separate RGB channels
                int blue = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int red = pixel & 0xff;

                // Modify LSB of each color channel if we have data left
                if (messageIndex < binaryData.length()) {
                    blue = (blue & ~1) | Character.getNumericValue(binaryData.charAt(messageIndex++));
                }
                if (messageIndex < binaryData.length()) {
                    green = (green & ~1) | Character.getNumericValue(binaryData.charAt(messageIndex++));
                }
                if (messageIndex < binaryData.length()) {
                    red = (red & ~1) | Character.getNumericValue(binaryData.charAt(messageIndex++));
                }

                // Combine channels back into pixel
                pixel = (blue << 16) | (green << 8) | red;
                image.setRGB(x, y, pixel);
            }
        }

        ImageIO.write(image, "bmp", new File(outputPath));
    }

    @Override
    public byte[] decode(String stegoImagePath) throws IOException {
        BufferedImage image = ImageIO.read(new File(stegoImagePath));
        StringBuilder binaryMessage = new StringBuilder();

        outer:
        for (int y = image.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);

                // Extract LSB from each channel
                int red = (pixel >> 16) & 1;
                int green = (pixel >> 8) & 1;
                int blue = pixel & 1;

                binaryMessage.append(red).append(green).append(blue);

                // Check for delimiter every 8 bits
                if (binaryMessage.length() % 8 == 0 && binaryMessage.length() >= DELIMITER.length * 8) {
                    byte[] currentBytes = binaryToBytes(binaryMessage.toString());
                    if (currentBytes != null) {
                        return currentBytes;
                    }
                }

                if (binaryMessage.length() > 10_000_000) { // Safety limit
                    break outer;
                }
            }
        }

        return null;
    }
}