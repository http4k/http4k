package org.http4k.multipart.part;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public abstract class Part extends PartMetaData implements Closeable {
    public final int length;

    public Part(String fieldName, boolean formField, String contentType, String fileName, Map<String, String> headers, int length) {
        super(fieldName, formField, contentType, fileName, headers);

        this.length = length;
    }

    public abstract InputStream getNewInputStream() throws IOException;

    public abstract boolean isInMemory();

    public abstract byte[] getBytes();

    public abstract String getString();
}
