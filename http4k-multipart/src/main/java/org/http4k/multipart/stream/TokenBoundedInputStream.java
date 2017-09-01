package org.http4k.multipart.stream;

import org.http4k.multipart.exceptions.StreamTooLongException;
import org.http4k.multipart.exceptions.TokenNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class TokenBoundedInputStream extends CircularBufferedInputStream {
    private final int maxStreamLength;

    public TokenBoundedInputStream(InputStream inputStream, int bufSize) {
        this(inputStream, bufSize, -1);
    }

    public TokenBoundedInputStream(InputStream inputStream, int bufSize, int maxStreamLength) {
        super(inputStream, bufSize);
        this.maxStreamLength = maxStreamLength;
    }

    /**
     * Consumes all bytes up to and including the matched endOfToken bytes.
     * Fills the buffer with all bytes excluding the endOfToken bytes.
     * Returns the number of bytes inserted into the buffer.
     *
     * @param endOfToken bytes that indicate the end of this token
     * @param buffer     fills this buffer with bytes _excluding_ the endOfToken
     * @param encoding   Charset for formatting error messages
     * @return number of bytes inserted into buffer
     * @throws IOException
     */
    public int getBytesUntil(byte[] endOfToken, byte[] buffer, Charset encoding) throws IOException {
        int bufferIndex = 0;
        int bufferLength = buffer.length;

        int b;
        while (true) {
            b = readFromStream();
            if (b < 0) {
                throw new TokenNotFoundException(
                    "Reached end of stream before finding Token <<" + new String(endOfToken, encoding) + ">>. " +
                        "Last " + endOfToken.length + " bytes read were " +
                        "<<" + getBytesRead(endOfToken, buffer, bufferIndex, encoding) + ">>");
            }
            if (bufferIndex >= bufferLength) {
                throw new TokenNotFoundException("Didn't find end of Token <<" + new String(endOfToken, encoding) + ">> " +
                    "within " + bufferLength + " bytes");
            }
            byte originalB = (byte) (b & 0x0FF);
            if (originalB == endOfToken[0]) {
                mark(endOfToken.length);
                if (matchToken(endOfToken, b)) {
                    return bufferIndex;
                }
                reset();
            }
            buffer[bufferIndex++] = originalB;
        }
    }

    private String getBytesRead(byte[] endOfToken, byte[] buffer, int bufferIndex, Charset encoding) {
        int index, length;
        if (bufferIndex - endOfToken.length > 0) {
            index = bufferIndex - endOfToken.length;
            length = endOfToken.length;
        } else {
            index = 0;
            length = bufferIndex;
        }
        return new String(buffer, index, length, encoding);
    }

    private boolean matchToken(byte[] token, int initialCharacter) throws IOException {
        int eotIndex = 0;
        while (initialCharacter > -1 && ((byte) initialCharacter == token[eotIndex]) && (++eotIndex) < token.length) {
            initialCharacter = readFromStream();
        }
        return eotIndex == token.length;
    }

    /**
     * Tries to match the token bytes at the current position. Only consumes bytes
     * if there is a match, otherwise the stream is unaffected.
     *
     * @param token The token being matched
     * @return true if the token is found (and the bytes have been consumed),
     * false if it isn't found (and the stream is unchanged)
     */
    public boolean matchInStream(byte[] token) throws IOException {
        mark(token.length);

        if (matchToken(token, readFromStream())) {
            return true;
        }

        reset();
        return false;
    }

    /**
     * returns a single byte from the Stream until the token is found. When the token is found,
     * -2 will be returned. The token will be consumed.
     *
     * @param token bytes that indicate the end of this token
     * @return the next byte in the stream, -1 if the underlying stream has finished,
     *         or -2 if the token is found. The token is consumed when it is matched.
     */
    public int readByteFromStreamUnlessTokenMatched(byte[] token) throws IOException {
        int b = readFromStream();
        if (((byte) b) == token[0]) {
            mark(token.length);

            if (matchToken(token, b)) {
                return -2;
            }

            reset();
        }
        return b;
    }


    private int readFromStream() throws IOException {
        if (maxStreamLength > -1 && cursor >= maxStreamLength) {
            throw new StreamTooLongException("Form contents was longer than " + maxStreamLength + " bytes");
        }
        return read();
    }

    public long currentByteIndex() {
        return cursor;
    }
}
