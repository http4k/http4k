package org.http4k.multipart.part;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

public class InMemoryPart extends Part {
    private final byte[] bytes; // not immutable
    private final Charset encoding;
    private String content = null;

    public InMemoryPart(PartMetaData original, byte[] bytes, Charset encoding) {
        super(original.fieldName, original.formField, original.contentType, original.fileName, original.headers, bytes.length);

        this.bytes = bytes;
        this.encoding = encoding;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getString() {
        if (content == null) {
            // not a threading problem because the following calculation will always return the same value
            // and if it happens to be calculated a couple of times and assigned to content a couple of times
            // that isn't the end of the world.
            content = new String(bytes, encoding);
        }
        return content;
    }

    public InputStream getNewInputStream() {
        return new ByteArrayInputStream(bytes);
    }

    @Override public boolean isInMemory() {
        return true;
    }

    public void close() {
        // do nothing
    }
}
