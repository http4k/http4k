package org.http4k.multipart.stream;

import org.http4k.multipart.exceptions.StreamTooLongException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class StreamUtil {
    public static String readStringFromInputStream(InputStream inputStream, Charset encoding, int maxPartContentSize) throws IOException {
        byte[] bytes = new byte[maxPartContentSize];
        int length = readAllBytesFromInputStream(inputStream, maxPartContentSize, bytes);
        return new String(bytes, 0, length, encoding);
    }

    public static byte[] readAllBytesFromInputStream(InputStream inputStream, int maxLength) throws IOException {
        byte[] bytes = new byte[maxLength];
        int length = readAllBytesFromInputStream(inputStream, maxLength, bytes);
        byte[] result = new byte[length];
        System.arraycopy(bytes, 0, result, 0, length);
        return result;
    }

    public static int readAllBytesFromInputStream(InputStream inputStream, int maxLength, byte[] bytes) throws IOException {
        int length = 0;

        while (true) {
            int count = inputStream.read(bytes, length, maxLength - length);
            if (count < 0) {
                inputStream.close();
                return length;
            }
            if (length >= maxLength) {
                inputStream.close();
                throw new StreamTooLongException("Part contents was longer than " + maxLength + " bytes");
            }
            length += count;
        }
    }
}
