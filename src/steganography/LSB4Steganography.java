package steganography;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class LSB4Steganography implements SteganographyInterface {

    @Override
    public void encode(String coverImagePath, byte[] data, String outputPath) throws IOException {
        BufferedImage image = ImageIO.read(new File(coverImagePath));
        String binaryData = bytesToBinary(data);

        // For LSB4, we need 4 times less pixels than bits
        if (binaryData.length() > image.getWidth() * image.getHeight() * 4) {
            throw new IllegalArgumentException("Data too large for cover image");
        }

        int messageIndex = 0;
        outer:
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (messageIndex >= binaryData.length()) {
                    break outer;
                }

                int pixel = image.getRGB(x, y);
                String fourBits = binaryData.substring(
                        messageIndex,
                        Math.min(messageIndex + 4, binaryData.length())
                );

                // Pad with zeros if less than 4 bits
                while (fourBits.length() < 4) {
                    fourBits += "0";
                }

                pixel = (pixel & ~0xF) | Integer.parseInt(fourBits, 2);
                image.setRGB(x, y, pixel);
                messageIndex += 4;
            }
        }

        ImageIO.write(image, "bmp", new File(outputPath));
    }

    @Override
    public byte[] decode(String stegoImagePath) throws IOException {
        BufferedImage image = ImageIO.read(new File(stegoImagePath));
        StringBuilder binaryMessage = new StringBuilder();

        outer:
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                String fourBits = String.format("%4s",
                                Integer.toBinaryString(pixel & 0xF))
                        .replace(' ', '0');
                binaryMessage.append(fourBits);

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
