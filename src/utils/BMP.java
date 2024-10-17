package utils;

import java.io.FileOutputStream;
import java.io.IOException;

public class BMP {
    private static final int BMP_HEADER_SIZE = 54;
    private static final int BMP_SIZE_OFFSET = 2;
    private static final int BMP_BYTES = 4;

    private final byte[] bmp;

    public BMP(byte[] bmp) {
        this.bmp = bmp;
    }


    public void dumpToFile(String filename) {
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(bmp);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
