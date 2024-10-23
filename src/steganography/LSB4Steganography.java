package steganography;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class LSB4Steganography implements SteganographyInterface {

    @Override
    public void encode(String imagePath, String message, String outputPath) throws IOException {
        BufferedImage image = ImageIO.read(new File(imagePath));
        String binaryMessage = textToBinary(message);

        // For LSB4, we need 4 times less pixels than bits
        if (binaryMessage.length() > image.getWidth() * image.getHeight() * 4) {
            throw new IllegalArgumentException("Message too large for image");
        }

        int messageIndex = 0;
        outer:
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (messageIndex >= binaryMessage.length()) {
                    break outer;
                }

                int pixel = image.getRGB(x, y);
                // Take next 4 bits from message
                String fourBits = binaryMessage.substring(
                        messageIndex,
                        Math.min(messageIndex + 4, binaryMessage.length())
                );
                // Pad with zeros if less than 4 bits
                while (fourBits.length() < 4) {
                    fourBits += "0";
                }

                // Clear 4 LSBs and set them to our message bits
                pixel = (pixel & ~0xF) | Integer.parseInt(fourBits, 2);
                image.setRGB(x, y, pixel);
                messageIndex += 4;
            }
        }

        ImageIO.write(image, "bmp", new File(outputPath));
    }

    @Override
    public String decode(String imagePath) throws IOException {
        BufferedImage image = ImageIO.read(new File(imagePath));
        StringBuilder binaryMessage = new StringBuilder();

        outer:
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                // Extract 4 LSBs
                String fourBits = String.format("%4s",
                                Integer.toBinaryString(pixel & 0xF))
                        .replace(' ', '0');
                binaryMessage.append(fourBits);

                // Try to decode the message at each step
                try {
                    String message = binaryToText(binaryMessage.toString());
                    if (message != null && !message.isEmpty()) {
                        return message;
                    }
                } catch (Exception ignored) {
                    // Continue if we can't decode yet
                }

                // Break if we've collected too many bits
                if (binaryMessage.length() > 1_000_000) {
                    break outer;
                }
            }
        }

        return null;
    }
}
