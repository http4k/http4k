package org.http4k.multipart.internal.stream;

public class BitFiddling {
    public static int getAnInt(byte b, int i) {
        return b & i;
    }
}
