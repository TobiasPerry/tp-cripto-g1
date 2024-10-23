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

        // Check if the image can hold the data
        if (binaryData.length() > image.getWidth() * image.getHeight()) {
            throw new IllegalArgumentException("Data too large for cover image");
        }

        int messageIndex = 0;
        for (int y = 0; y < image.getHeight() && messageIndex < binaryData.length(); y++) {
            for (int x = 0; x < image.getWidth() && messageIndex < binaryData.length(); x++) {
                int pixel = image.getRGB(x, y);
                pixel = (pixel & ~1) | Character.getNumericValue(binaryData.charAt(messageIndex));
                image.setRGB(x, y, pixel);
                messageIndex++;
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
                binaryMessage.append(pixel & 1);

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