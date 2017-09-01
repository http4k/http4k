package org.http4k.multipart.exceptions;

public class ParseError extends RuntimeException {
    public ParseError(Exception e) {
        super(e);
    }

    public ParseError(String message) {
        super(message);
    }
}
