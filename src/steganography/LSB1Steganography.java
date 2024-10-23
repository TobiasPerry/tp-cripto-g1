package steganography;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class LSB1Steganography implements SteganographyInterface {

    @Override
    public void encode(String imagePath, String message, String outputPath) throws IOException {
        BufferedImage image = ImageIO.read(new File(imagePath));
        String binaryMessage = textToBinary(message);

        if (binaryMessage.length() > image.getWidth() * image.getHeight()) {
            throw new IllegalArgumentException("Message too large for image");
        }

        int messageIndex = 0;
        for (int y = 0; y < image.getHeight() && messageIndex < binaryMessage.length(); y++) {
            for (int x = 0; x < image.getWidth() && messageIndex < binaryMessage.length(); x++) {
                int pixel = image.getRGB(x, y);
                pixel = (pixel & ~1) | Character.getNumericValue(binaryMessage.charAt(messageIndex));
                image.setRGB(x, y, pixel);
                messageIndex++;
            }
        }

        ImageIO.write(image, "bmp", new File(outputPath));
    }

    @Override
    public String decode(String imagePath) throws IOException {
        BufferedImage image = ImageIO.read(new File(imagePath));
        StringBuilder binaryMessage = new StringBuilder();

        // Extract LSBs until we find the delimiter or reach the end
        outer:
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                binaryMessage.append(pixel & 1);

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