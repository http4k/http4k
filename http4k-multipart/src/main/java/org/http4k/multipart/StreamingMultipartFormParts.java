package org.http4k.multipart;

import org.apache.commons.fileupload.util.ParameterParser;
import org.http4k.multipart.exceptions.AlreadyClosedException;
import org.http4k.multipart.exceptions.ParseError;
import org.http4k.multipart.exceptions.TokenNotFoundException;
import org.http4k.multipart.part.StreamingPart;
import org.http4k.multipart.stream.TokenBoundedInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>
 */
public class StreamingMultipartFormParts implements Iterable<StreamingPart> {
    private static final int DEFAULT_BUFSIZE = 4096;


    /**
     * The Carriage Return ASCII character value.
     */
    public static final byte CR = 0x0D;

    /**
     * The Line Feed ASCII character value.
     */
    public static final byte LF = 0x0A;

    /**
     * The dash (-) ASCII character value.
     */
    public static final byte DASH = 0x2D;

    /**
     * The maximum length of all headers
     */
    public static final int HEADER_SIZE_MAX = 10 * 1024;

    /**
     * A byte sequence that that follows a delimiter that will be
     * followed by an encapsulation (<code>CRLF</code>).
     */
    protected static final byte[] FIELD_SEPARATOR = {CR, LF};

    /**
     * A byte sequence that that follows a delimiter of the last
     * encapsulation in the stream (<code>--</code>).
     */
    protected static final byte[] STREAM_TERMINATOR = {DASH, DASH};

    private final TokenBoundedInputStream inputStream;
    private final Charset encoding;
    private final Iterator<StreamingPart> iterator;

    private byte[] boundary;
    private byte[] boundaryWithPrefix;
    private MultipartFormStreamState state;
    // yes yes, I should use a stack or something for this
    private String mixedName = null;
    private byte[] oldBoundary = null;
    private byte[] oldBoundaryWithPrefix;

    /**
     * Uses the <code>boundary</code> to parse the <code>encoding</code> coded <code>inputStream</code>,
     * returning an <code>Iterable</code> of <code>StreamingPart</code>s.
     * <br/>
     * You need to look after closing the inputStream yourself.
     * <br/>
     * The Iterable can throw a number of <code>ParseError</code> Exceptions that you should look
     * out for. Sorry :( They will only happen if something goes wrong with the parsing, which
     * it shouldn't ;)
     *
     * @param boundary    byte array defining the boundary between parts, usually found in the
     *                    Content-Type header of an HTTP request
     * @param inputStream of the body of an HTTP request - will be parsed as <code>multipart/form-data</code>
     * @param encoding    of the body of the HTTP request
     * @return an <code>Iterable&lt;StreamingPart></code> that you can for() through to get each part
     * @throws IOException
     */
    public static Iterable<StreamingPart> parse(byte[] boundary, InputStream inputStream, Charset encoding) {
        return new StreamingMultipartFormParts(boundary, encoding, new TokenBoundedInputStream(inputStream, DEFAULT_BUFSIZE));
    }

    public static Iterable<StreamingPart> parse(byte[] boundary, InputStream inputStream, Charset encoding, int maxStreamLength) {
        return new StreamingMultipartFormParts(boundary, encoding, new TokenBoundedInputStream(inputStream, DEFAULT_BUFSIZE, maxStreamLength));
    }

    private StreamingMultipartFormParts(byte[] boundary, Charset encoding, TokenBoundedInputStream tokenBoundedInputStream) {
        this.boundary = prependBoundaryWithStreamTerminator(boundary);
        this.encoding = encoding;
        this.inputStream = tokenBoundedInputStream;

        this.boundaryWithPrefix = addPrefixToBoundary(this.boundary);

        state = MultipartFormStreamState.findBoundary;
        iterator = new StreamingMulipartFormPartIterator();
    }

    @Override public Iterator<StreamingPart> iterator() {
        return iterator;
    }

    private byte[] addPrefixToBoundary(byte[] boundary) {
        byte[] b = new byte[boundary.length + FIELD_SEPARATOR.length]; // in apache they just use BOUNDARY_PREFIX
        System.arraycopy(boundary, 0, b, 2, boundary.length);
        System.arraycopy(FIELD_SEPARATOR, 0, b, 0, FIELD_SEPARATOR.length);
        return b;
    }

    public static byte[] prependBoundaryWithStreamTerminator(byte[] boundary) {
        byte[] actualBoundary = new byte[boundary.length + 2];
        System.arraycopy(STREAM_TERMINATOR, 0, actualBoundary, 0, 2);
        System.arraycopy(boundary, 0, actualBoundary, 2, boundary.length);
        return actualBoundary;
    }

    private void findBoundary() throws IOException {
        if (state == MultipartFormStreamState.findPrefix) {
            if (!inputStream.matchInStream(FIELD_SEPARATOR)) {
                throw new TokenNotFoundException("Boundary must be proceeded by field separator, but didn't find it");
            }
            state = MultipartFormStreamState.findBoundary;
        }

        if (state == MultipartFormStreamState.findBoundary) {
            if (!inputStream.matchInStream(boundary)) {
                throw new TokenNotFoundException("Boundary not found <<" + new String(boundary, encoding) + ">>");
            }
        }

        state = MultipartFormStreamState.boundaryFound;
        if (inputStream.matchInStream(STREAM_TERMINATOR)) {
            if (inputStream.matchInStream(FIELD_SEPARATOR)) {
                if (mixedName != null) {
                    boundary = oldBoundary;
                    boundaryWithPrefix = oldBoundaryWithPrefix;
                    mixedName = null;

                    state = MultipartFormStreamState.findBoundary;
                    findBoundary();
                } else {
                    state = MultipartFormStreamState.eos;
                }
            } else {
                throw new TokenNotFoundException("Stream terminator must be followed by field separator, but didn't find it");
            }
        } else {
            if (!inputStream.matchInStream(FIELD_SEPARATOR)) {
                throw new TokenNotFoundException("Boundary must be followed by field separator, but didn't find it");
            } else {
                state = MultipartFormStreamState.header;
            }
        }
    }

    private StreamingPart parseNextPart() throws IOException {
        findBoundary();
        if (state == MultipartFormStreamState.header) {
            return parsePart();
        } else {
            return null;
        }
    }

    private StreamingPart parsePart() throws IOException {
        Map<String, String> headers = parseHeaderLines();

        String contentType = headers.get("Content-Type");
        if (contentType != null && contentType.startsWith("multipart/mixed")) {
            Map<String, String> contentDisposition = new ParameterParser().parse(headers.get("Content-Disposition"), ';');
            Map<String, String> contentTypeParams = new ParameterParser().parse(contentType, ';');

            mixedName = trim(contentDisposition.get("name"));

            oldBoundary = boundary;
            oldBoundaryWithPrefix = boundaryWithPrefix;
            boundary = (new String(STREAM_TERMINATOR, encoding) + trim(contentTypeParams.get("boundary"))).getBytes(encoding);
            boundaryWithPrefix = addPrefixToBoundary(this.boundary);

            state = MultipartFormStreamState.findBoundary;

            return parseNextPart();
        } else {
            Map<String, String> contentDisposition = new ParameterParser().parse(headers.get("Content-Disposition"), ';');
            String fieldName = contentDisposition.containsKey("attachment") ? mixedName : trim(contentDisposition.get("name"));
            String filename = filenameFromMap(contentDisposition);

            return new StreamingPart(
                fieldName,
                !contentDisposition.containsKey("filename"),
                contentType,
                filename,
                new BoundedInputStream(), headers);
        }
    }

    private String filenameFromMap(Map<String, String> contentDisposition) {
        if (contentDisposition.containsKey("filename")) {
            String filename = contentDisposition.get("filename");
            return trim(filename == null ? "" : filename);
        } else {
            return null;
        }
    }


    private String trim(String string) {
        if (string != null) {
            return string.trim();
        }
        return null;
    }

    private Map<String, String> parseHeaderLines() throws IOException {
        assertStateIs(MultipartFormStreamState.header);

        Map<String, String> result = new HashMap<>();
        String previousHeaderName = null;
        long maxByteIndexForHeader = inputStream.currentByteIndex() + HEADER_SIZE_MAX;
        while (inputStream.currentByteIndex() < maxByteIndexForHeader) {
            String header = readStringFromStreamUntilMatched(inputStream, FIELD_SEPARATOR, (int) (maxByteIndexForHeader - inputStream.currentByteIndex()), encoding);
            if (header.equals("")) {
                state = MultipartFormStreamState.contents;
                return result;
            }
            if (header.matches("\\s+.*")) {
                result.put(previousHeaderName, result.get(previousHeaderName) + "; " + header.trim());
            } else {
                int index = header.indexOf(":");
                if (index < 0) {
                    throw new ParseError("Header didn't include a colon <<" + header + ">>");
                } else {
                    previousHeaderName = header.substring(0, index).trim();
                    result.put(previousHeaderName, header.substring(index + 1).trim());
                }
            }
        }
        throw new TokenNotFoundException("Didn't find end of Header section within " + HEADER_SIZE_MAX + " bytes");
    }

    public static String readStringFromStreamUntilMatched(TokenBoundedInputStream tokenBoundedInputStream, byte[] endOfToken, int maxStringSizeInBytes, Charset encoding) throws IOException {
        // very inefficient search!
        byte[] buffer = new byte[maxStringSizeInBytes];
        int bytes = tokenBoundedInputStream.getBytesUntil(endOfToken, buffer, encoding);
        return new String(buffer, 0, bytes, encoding);
    }

    public class StreamingMulipartFormPartIterator implements Iterator<StreamingPart> {
        private boolean nextIsKnown;
        private StreamingPart currentPart;

        @Override public boolean hasNext() {
            if (nextIsKnown) {
                return !isEndOfStream();
            }
            nextIsKnown = true;

            if (state == MultipartFormStreamState.contents) {
                try {
                    currentPart.inputStream.close();
                } catch (Exception e) {
                /* ???? */
                }
            }

            currentPart = safelyParseNextPart();

            return !isEndOfStream();
        }

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration
         * @throws NoSuchElementException if the iteration has no more elements
         * @throws ParseError             if there was a problem parsing the stream
         */
        @Override public StreamingPart next() {
            if (nextIsKnown) {
                if (isEndOfStream()) {
                    throw new NoSuchElementException("No more parts in this MultipartForm");
                }
                nextIsKnown = false;
            } else {

                if (state == MultipartFormStreamState.contents) {
                    try {
                        currentPart.inputStream.close();
                    } catch (Exception e) {
                /* ???? */
                    }
                }

                currentPart = safelyParseNextPart();
                if (isEndOfStream()) {
                    throw new NoSuchElementException("No more parts in this MultipartForm");
                }
            }
            return currentPart;
        }

        private StreamingPart safelyParseNextPart() {
            try {
                return parseNextPart();
            } catch (IOException e) {
                nextIsKnown = true;
                currentPart = null;
                throw new ParseError(e);
            }
        }

        private boolean isEndOfStream() {
            return currentPart == null;
        }

    }

    private class BoundedInputStream extends InputStream {

        boolean endOfStream = false;
        boolean closed = false;

        @Override public int read() throws IOException {
            if (closed) {
                throw new AlreadyClosedException();
            }

            if (endOfStream) {
                return -1;
            }

            return readNextByte();
        }

        private int readNextByte() throws IOException {
            int result = inputStream.readByteFromStreamUnlessTokenMatched(boundaryWithPrefix);
            if (result == -1) {
                state = MultipartFormStreamState.findPrefix;
                endOfStream = true;
                return -1;
            }
            if (result == -2) {
                state = MultipartFormStreamState.boundaryFound;
                endOfStream = true;
                return -1; // inputStream.read(byte b[], int off, int len) checks for exactly -1
            }
            return result;
        }

        @Override public void close() {
            closed = true;
            if (!endOfStream) {
                try {
                    //noinspection StatementWithEmptyBody
                    while (readNextByte() >= 0) {
                        // drop unwanted bytes :(
                    }
                } catch (IOException e) {
                    endOfStream = true;
                    throw new ParseError(e);
                }
            }
        }
    }

    private void assertStateIs(MultipartFormStreamState expectedState) {
        if (expectedState != state) {
            throw new IllegalStateException("Expected state " + expectedState + " but got " + state);
        }
    }

    private enum MultipartFormStreamState {
        findPrefix, findBoundary, boundaryFound, eos, header, contents, error
    }
}
