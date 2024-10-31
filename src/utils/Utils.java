package utils;

public class Utils {

    public static String getFileExtensionFromPath(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf('.');
        if (lastIndexOfDot == -1 || lastIndexOfDot == fileName.length() - 1) {
            return ""; // No extension or dot at the end
        }
        return fileName.substring(lastIndexOfDot) + '\0';
    }

}
