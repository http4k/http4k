package io.cloudevents.jackson;

import java.util.regex.Pattern;

public class CloudEventsContentType {
    private static final Pattern JSON_CONTENT_TYPE_PATTERN = Pattern.compile("^(application|text)\\/([a-zA-Z]+\\+)?json(;.*)*$");

    public static boolean dataIsJsonContentType(String contentType) {
        // If content type, spec states that we should assume is json
        return contentType == null || JSON_CONTENT_TYPE_PATTERN.matcher(contentType).matches();
    }
}
