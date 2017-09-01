package org.http4k.multipart.stream;

public class BitFiddling {
    public static int getAnInt(byte b, int i) {
        return b & i;
    }
}
