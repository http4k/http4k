package org.http4k.multipart.exceptions;

import java.io.IOException;

public class StreamTooLongException extends IOException {
    public StreamTooLongException(String message) {
        super(message);
    }
}
