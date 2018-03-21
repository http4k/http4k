package org.http4k;

class MimeTypeEntry {
    private String type;

    public MimeTypeEntry(String mime_type) {
        type = mime_type;
    }

    public String getMIMEType() {
        return type;
    }
}
