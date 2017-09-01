package org.http4k.multipart.stream;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.InvalidMarkException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@SuppressWarnings("ALL")
public class CircularBufferedInputStreamTest {

    @Test
    public void returns_a_byte_at_a_time() throws Exception {
        byte[] bytes = "hello my name is Tiest".getBytes();
        InputStream inputStream = createInputStream(bytes, 3);

        for (byte b : bytes) {
            int read = inputStream.read();
            assertThat(read, equalTo((int) b));
        }
        assertThat(inputStream.read(), equalTo(-1));
    }

    @Test
    public void returns_bytes() throws Exception {
        byte[] bytes = "hello my name is Tiest".getBytes();
        InputStream inputStream = createInputStream(bytes, 3);

        byte[] buffer = new byte[2];
        int read = inputStream.read(buffer, 0, 2);

        assertThat(read, equalTo(2));
        assertThat((char) buffer[0], equalTo('h'));
        assertThat((char) buffer[1], equalTo('e'));

        buffer = new byte[5];
        read = inputStream.read(buffer, 0, 5);

        assertThat(read, equalTo(5));
        assertThat((char) buffer[0], equalTo('l'));
        assertThat((char) buffer[1], equalTo('l'));
        assertThat((char) buffer[2], equalTo('o'));
        assertThat((char) buffer[3], equalTo(' '));
        assertThat((char) buffer[4], equalTo('m'));

        buffer = new byte[50];
        read = inputStream.read(buffer, 10, 40);
        assertThat(read, equalTo(15));
        assertThat((char) buffer[10], equalTo('y'));
        assertThat((char) buffer[24], equalTo('t'));

        read = inputStream.read(buffer, 10, 40);
        assertThat(read, equalTo(-1));
    }

    @Test
    public void cannot_mark_further_then_buffer_size() throws Exception {
        byte[] bytes = "hello my name is Tiest".getBytes();
        InputStream inputStream = createInputStream(bytes, 3);

        try {
            inputStream.mark(5);
            fail("can't have readlimit larger than buffer");
        } catch (ArrayIndexOutOfBoundsException e) {
            assertThat(e.getMessage(), containsString("Readlimit (5) cannot be bigger than buffer size (4)"));
        }
    }

    @Test
    public void marks_and_resets() throws Exception {
        byte[] bytes = "My name is Tiest don't you know".getBytes();
        InputStream inputStream = createInputStream(bytes, 7);

        inputStream.read(); // M
        inputStream.read(); // y
        inputStream.read(); // ' '

        inputStream.mark(8);

        int marked = inputStream.read(); // n
        inputStream.read(); // a
        inputStream.read(); // m
        inputStream.read(); // e

        inputStream.reset();

        int secondMark = inputStream.read(); // n
        assertThat((char) secondMark, equalTo((char) marked));

        inputStream.read(); // a
        inputStream.read(); // m
        inputStream.read(); // e

        inputStream.reset();

        assertThat((char) inputStream.read(), equalTo((char) secondMark));
        inputStream.read(); // a
        inputStream.read(); // m
        inputStream.read(); // e
        inputStream.read(); // ' '
        inputStream.read(); // i
        inputStream.read(); // s

        inputStream.mark(8);

        int thirdMark = inputStream.read(); // ' '
        inputStream.read(); // T
        inputStream.read(); // i
        inputStream.read(); // e

        inputStream.reset();

        assertThat((char) inputStream.read(), equalTo((char) thirdMark));
    }

    @Test
    public void resetting_after_reading_past_readlimit_fails() throws Exception {
        byte[] bytes = "My name is Tiest don't you know".getBytes();
        InputStream inputStream = createInputStream(bytes, 7);

        inputStream.read(); // M
        inputStream.read(); // y
        inputStream.read(); // ' '

        inputStream.mark(2);

        inputStream.read(); // n
        inputStream.read(); // a
        inputStream.read(); // m
        inputStream.read(); // e
        inputStream.read(); //
        inputStream.read(); // i - reads new values into buffer, reseting the leftBound/mark

        try {
            inputStream.reset();
            fail("Have read past readlimit, should fail");
        } catch (InvalidMarkException e) {
            assertThat(e.getMessage(), equalTo(null));
        }
    }

    @Test
    public void resetting_after_reading_past_readlimit_fails_2() throws Exception {
        byte[] bytes = "My name is Tiest don't you know".getBytes();
        InputStream inputStream = createInputStream(bytes, 7);

        inputStream.read(); // M
        inputStream.read(); // y
        inputStream.read(); // ' '
        inputStream.read(); // n
        inputStream.read(); // a
        inputStream.read(); // m

        inputStream.mark(2);
        int marked = inputStream.read(); // e - reads new values into buffer, reseting the leftBound/mark
        inputStream.read(); // ' '

        inputStream.reset();
        assertThat((char) inputStream.read(), equalTo((char) marked));

        inputStream.read(); // ' '
        inputStream.read(); // i
        inputStream.read(); // s
        try {
            inputStream.reset();
            fail("Have read past readlimit, should fail");
        } catch (InvalidMarkException e) {
            assertThat(e.getMessage(), equalTo(null));
        }
    }

    private InputStream createInputStream(byte[] bytes, int bufSize) {
        return new CircularBufferedInputStream(new ByteArrayInputStream(bytes), bufSize);
//        return new BufferedInputStream(new ByteArrayInputStream(bytes), bufSize);
    }
}