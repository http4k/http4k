package org.http4k.multipart.exceptions;

import java.io.IOException;

public class TokenNotFoundException extends IOException {
    public TokenNotFoundException(String message) {
        super(message);
    }
}
