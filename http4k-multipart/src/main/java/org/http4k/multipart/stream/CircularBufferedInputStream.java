package org.http4k.multipart.stream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.InvalidMarkException;

public class CircularBufferedInputStream extends InputStream {
    private static final boolean DEBUG = false;
    private final int bufferSize;
    private final long bufferIndexMask;
    private final byte[] buffer;
    private final InputStream inputStream;

    protected long cursor;
    private long rightBounds;
    private long leftBounds;
    private long readLimit;
    private boolean markInvalid;
    private boolean EOS;

    public CircularBufferedInputStream(InputStream inputStream, int maxExpectedBufSize) {
        this.bufferSize = Integer.highestOneBit(maxExpectedBufSize) * 2;
        this.bufferIndexMask = bufferSize - 1;
        this.buffer = new byte[bufferSize];
        this.inputStream = inputStream;
        this.cursor = 0;
        this.rightBounds = 0;
        this.leftBounds = 0;
        this.readLimit = 0;
        this.markInvalid = false;
        this.EOS = false;
    }

    @Override public int read() throws IOException {
        dumpState(">>> READ");

        if (EOS) {
            return -1;
        }
        int result = read1();

        dumpState("<<< READ");

        return result;
    }

    private int read1() throws IOException {
        while (cursor == rightBounds) {
            if (!readMore()) {
                return -1;
            }
        }
        return buffer[(int) (cursor++ & bufferIndexMask)] & 0x0FF;
    }

    @Override public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        if (EOS) {
            return -1;
        }

        for (int i = 0; i < len; i++) {
            int result = read1();
            if (result == -1) {
                return i;
            }
            b[i + off] = (byte) result;
        }

        return len;
    }

    private boolean readMore() throws IOException {
        long rightIndex = rightBounds & bufferIndexMask;
        long leftIndex = leftBounds & bufferIndexMask;

        int readThisManyBytes = leftIndex > rightIndex ? (int) (leftIndex - rightIndex) : (int) (buffer.length - rightIndex);

        int readBytes = inputStream.read(
            buffer,
            (int) rightIndex,
            readThisManyBytes
        );

        if (readBytes < 0) {
            EOS = true;
            return false;
        }
        rightBounds += readBytes;

        // move mark if past readLimit
        if (cursor - leftBounds > readLimit) {
            leftBounds = cursor;
            readLimit = 0;
            markInvalid = true;
        }

        return true;
    }

    @Override public int available() throws IOException {
        return (int) (rightBounds - cursor);
    }

    @Override public boolean markSupported() {
        return true;
    }

    @Override public synchronized void reset() throws IOException {
        dumpState(">>> RESET");

        if (markInvalid) {
            // The mark has been moved because you have read past your readlimit
            throw new InvalidMarkException();
        }
        cursor = leftBounds;
        readLimit = 0;
        markInvalid = false;

        dumpState("<<< RESET");
    }

    @Override public synchronized void mark(int readlimit) {
        dumpState(">>> MARK");

        if (readlimit > bufferSize) {
            throw new ArrayIndexOutOfBoundsException(String.format("Readlimit (%d) cannot be bigger than buffer size (%d)", readlimit, bufferSize));
        }
        leftBounds = cursor;
        markInvalid = false;
        this.readLimit = readlimit;

        dumpState("<<< MARK");
    }

    private void dumpState(String description) {
        if (DEBUG) {
            System.out.println(description);
            System.out.println(
                "l=" + leftBounds + "(" + (leftBounds & bufferIndexMask) + ") " +
                    "c=" + cursor + "(" + (cursor & bufferIndexMask) + ") " +
                    "r=" + rightBounds + "(" + (rightBounds & bufferIndexMask) + ") '" +
                    "rl=" + readLimit + " " +
                    (char) buffer[(int) (cursor & bufferIndexMask)] + "'");
            for (byte b : buffer) {
                System.out.print((char) b);
            }
            System.out.println();
        }
    }

}
