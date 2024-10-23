package steganography;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public final class LSB4Steganography implements SteganographyInterface {
    @Override
    public void encode(String coverImagePath, byte[] data, String outputPath) throws IOException {
        BufferedImage image = ImageIO.read(new File(coverImagePath));
        String binaryData = bytesToBinary(data);

        // Each pixel can store 12 bits (4 in each RGB channel)
        if (binaryData.length() > image.getWidth() * image.getHeight() * 12) {
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

                // Modify 4 LSBs of each color channel if we have data left
                if (messageIndex + 4 <= binaryData.length()) {
                    red = (red & ~0xF) | Integer.parseInt(binaryData.substring(messageIndex, messageIndex + 4), 2);
                    messageIndex += 4;
                }
                if (messageIndex + 4 <= binaryData.length()) {
                    green = (green & ~0xF) | Integer.parseInt(binaryData.substring(messageIndex, messageIndex + 4), 2);
                    messageIndex += 4;
                }
                if (messageIndex + 4 <= binaryData.length()) {
                    blue = (blue & ~0xF) | Integer.parseInt(binaryData.substring(messageIndex, messageIndex + 4), 2);
                    messageIndex += 4;
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

                // Extract 4 LSBs from each channel
                String redBits = String.format("%4s", Integer.toBinaryString((pixel >> 16) & 0xF)).replace(' ', '0');
                String greenBits = String.format("%4s", Integer.toBinaryString((pixel >> 8) & 0xF)).replace(' ', '0');
                String blueBits = String.format("%4s", Integer.toBinaryString(pixel & 0xF)).replace(' ', '0');

                binaryMessage.append(redBits).append(greenBits).append(blueBits);

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