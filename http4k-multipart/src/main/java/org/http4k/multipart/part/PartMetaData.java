package org.http4k.multipart.part;

import java.util.Collections;
import java.util.Map;

public abstract class PartMetaData {
    public final String fieldName;
    public final boolean formField;
    public final String contentType;
    public final String fileName;
    public final Map<String, String> headers;

    public PartMetaData(String fieldName, boolean formField, String contentType, String fileName, Map<String, String> headers) {
        this.fieldName = fieldName;
        this.formField = formField;
        this.contentType = contentType;
        this.fileName = fileName;
        this.headers = Collections.unmodifiableMap(headers);
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean isFormField() {
        return formField;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void sink() {
        throw new UnsupportedOperationException("sink not implemented");
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

}
