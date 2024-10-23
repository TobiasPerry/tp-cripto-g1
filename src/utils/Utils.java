package utils;

public class Utils {


    public static byte[] prepareFileForStenographyWithoutEncryption(byte[] fileToStenographBytes, String fileToStenograph){
        byte[] toStenograph = new byte[fileToStenographBytes.length + 24];

        toStenograph[0] = (byte) (fileToStenographBytes.length & 0xFF);
        toStenograph[1] = (byte) ((fileToStenographBytes.length >> 8) & 0xFF);
        toStenograph[2] = (byte) ((fileToStenographBytes.length >> 16) & 0xFF);
        toStenograph[3] = (byte) ((fileToStenographBytes.length >> 24) & 0xFF);

        System.arraycopy(fileToStenographBytes, 0, toStenograph, 4, fileToStenographBytes.length);

        // Add the file extension
        byte[] fileExtensionBytes = getFileExtension(fileToStenograph).getBytes();

        System.arraycopy(fileExtensionBytes, 0, toStenograph, fileToStenographBytes.length + 4, fileExtensionBytes.length);

        return toStenograph;
    }

    private static String getFileExtension(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf('.');
        if (lastIndexOfDot == -1 || lastIndexOfDot == fileName.length() - 1) {
            return ""; // No extension or dot at the end
        }
        return fileName.substring(lastIndexOfDot);
    }

}
