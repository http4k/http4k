package org.http4k.multipart.part;

import org.http4k.multipart.stream.StreamUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class StreamingPart extends PartMetaData {
    public final InputStream inputStream;

    public StreamingPart(String fieldName, boolean formField, String contentType, String fileName, InputStream inputStream, Map<String, String> headers) {
        super(fieldName, formField, contentType, fileName, headers);
        this.inputStream = inputStream;
    }

    public String getContentsAsString() throws IOException {
        return getContentsAsString(StandardCharsets.UTF_8, 4096);
    }

    public String getContentsAsString(Charset encoding, int maxPartContentSize) throws IOException {
        return StreamUtil.readStringFromInputStream(inputStream, encoding, maxPartContentSize);
    }

}
