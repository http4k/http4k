package org.http4k.multipart;

import kotlin.Pair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class ValidMultipartFormBuilder {
    private final Deque<byte[]> boundary = new ArrayDeque<>();
    private final ByteArrayOutputStream builder = new ByteArrayOutputStream();
    private final Charset encoding;

    public ValidMultipartFormBuilder(String boundary) {
        this(boundary.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    public ValidMultipartFormBuilder(byte[] boundary, Charset encoding) {
        this.encoding = encoding;
        this.boundary.push(StreamingMultipartFormParts.prependBoundaryWithStreamTerminator(boundary));
    }

    public byte[] build() {
        try {
            builder.write(boundary.peek());
            builder.write(StreamingMultipartFormParts.STREAM_TERMINATOR);
            builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR);
            return builder.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ValidMultipartFormBuilder field(String name, String value) {
        part(value,
                new Pair("Content-Disposition", asList(new Pair("form-data", null), new Pair("name", name)))
        );
        return this;
    }

    private void appendHeader(final String headerName, List<Pair<String, String>> pairs) {
        try {
            String headers = headerName + ": " + pairs.stream().map((pair) -> {
                if (pair.getSecond() != null) {
                    return pair.getFirst() + "=\"" + pair.getSecond() + "\"";
                }
                return pair.getFirst();
            }).collect(Collectors.joining("; "));

            builder.write(headers.getBytes(encoding));
            builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ValidMultipartFormBuilder part(String contents, Pair<String, List<Pair<String, String>>>... headers) {
        try {
            builder.write(boundary.peek());
            builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR);
            asList(headers).forEach(header -> {
                appendHeader(header.getFirst(), header.getSecond());
            });
            builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR);
            builder.write(contents.getBytes(encoding));
            builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR);
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ValidMultipartFormBuilder file(String fieldName, String filename, String contentType, String contents) {
        part(contents,
                new Pair("Content-Disposition", asList(new Pair("form-data", null), new Pair("name", fieldName), new Pair("filename", filename))),
                new Pair("Content-Type", asList(new Pair(contentType, null)))
        );
        return this;
    }

    public ValidMultipartFormBuilder rawPart(String raw) {
        try {
            builder.write(boundary.peek());
            builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR);
            builder.write(raw.getBytes(encoding));
            builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR);
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ValidMultipartFormBuilder startMultipart(String multipartFieldName, String subpartBoundary) {
        try {
            builder.write(boundary.peek());
            builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR);
            appendHeader("Content-Disposition", asList(new Pair("form-data", null), new Pair("name", multipartFieldName)));
            appendHeader("Content-Type", asList(new Pair("multipart/mixed", null), new Pair("boundary", subpartBoundary)));
            builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR);
            boundary.push((new String(StreamingMultipartFormParts.STREAM_TERMINATOR, encoding) + subpartBoundary).getBytes(encoding));
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ValidMultipartFormBuilder attachment(String fileName, String contentType, String contents) {
        part(contents,
                new Pair("Content-Disposition", asList(new Pair("attachment", null), new Pair("filename", fileName))),
                new Pair("Content-Type", asList(new Pair(contentType, null)))
        );
        return this;
    }

    public ValidMultipartFormBuilder endMultipart() {
        try {
            builder.write(boundary.pop());
            builder.write(StreamingMultipartFormParts.STREAM_TERMINATOR);
            builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR);
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
