package org.http4k.multipart;

import kotlin.Pair;
import org.http4k.multipart.exceptions.ParseError;
import org.http4k.multipart.exceptions.TokenNotFoundException;
import org.http4k.multipart.part.StreamingPart;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class StreamingMultipartFormSadTests {

    @Test
    public void failsWhenNoBoundaryInStream() throws Exception {
        String boundary = "---1234";
        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, "No boundary anywhere".getBytes());

        assertParseErrorWrapsTokenNotFound(form, "Boundary not found <<-----1234>>");

        form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, "No boundary anywhere".getBytes());

        try {
            form.next();
            fail("Should have thrown ParseError");
        } catch (ParseError e) {
            assertThat(e.getCause().getClass(), equalTo(TokenNotFoundException.class));
            assertThat(e.getCause().getMessage(), equalTo("Boundary not found <<-----1234>>"));
        }

    }

    @Test
    public void failsWhenGettingNextPastEndOfParts() throws Exception {
        String boundary = "-----1234";
        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .file("aFile", "file.name", "application/octet-stream", "File contents here")
            .file("anotherFile", "your.name", "application/octet-stream", "Different file contents here").build());

        form.next(); // aFile
        form.next(); // anotherFile
        try {
            form.next(); // no such element
            fail("Should have thrown NoSuchElementException");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    @Test
    public void failsWhenGettingNextPastEndOfPartsAfterHasNext() throws Exception {
        String boundary = "-----1234";
        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .file("aFile", "file.name", "application/octet-stream", "File contents here")
            .file("anotherFile", "your.name", "application/octet-stream", "Different file contents here").build());

        form.next(); // aFile
        form.next(); // anotherFile
        assertThat(form.hasNext(), equalTo(false));
        try {
            form.next(); // no such element
            fail("Should have thrown NoSuchElementException");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    @Test
    public void partHasNoHeaders() throws Exception {
        String boundary = "-----2345";
        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .field("multi", "value0")
            .rawPart("" + StreamingMultipartFormHappyTests.CR_LF + "value with no headers")
            .field("multi", "value2")
            .build());

        form.next();
        StreamingPart StreamingPart = form.next();
        assertThat(StreamingPart.getFieldName(), nullValue());
        assertThat(StreamingPart.getContentsAsString(), equalTo("value with no headers"));
        assertThat(StreamingPart.getHeaders().size(), equalTo(0));
        assertThat(StreamingPart.isFormField(), equalTo(true));
        assertThat(StreamingPart.getFileName(), nullValue());
        form.next();
    }

    @Test
    public void overwritesPartHeaderIfHeaderIsRepeated() throws Exception {
        String boundary = "-----2345";
        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .part("contents of StreamingPart",
                new Pair("Content-Disposition", asList(new Pair("form-data", null), new Pair("bit", "first"), new Pair("name", "first-name"))),
                new Pair("Content-Disposition", asList(new Pair("form-data", null), new Pair("bot", "second"), new Pair("name", "second-name"))))
            .build());

        StreamingPart StreamingPart = form.next();
        assertThat(StreamingPart.getFieldName(), equalTo("second-name"));
        assertThat(StreamingPart.getHeaders().get("Content-Disposition"),
            equalTo("form-data; bot=\"second\"; name=\"second-name\""));
    }

    @Test
    public void failsIfFoundBoundaryButNoFieldSeparator() throws Exception {
        String boundary = "---2345";

        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + // no CR_LF
            "Content-Disposition: form-data; name=\"name\"" + StreamingMultipartFormHappyTests.CR_LF +
            "" + StreamingMultipartFormHappyTests.CR_LF +
            "value" + StreamingMultipartFormHappyTests.CR_LF +
            "-----2345--" + StreamingMultipartFormHappyTests.CR_LF).getBytes());

        assertParseErrorWrapsTokenNotFound(form, "Boundary must be followed by field separator, but didn't find it");
    }

    @Test
    public void failsIfHeaderMissingFieldSeparator() throws Exception {
        String boundary = "---2345";

        assertParseError(StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + StreamingMultipartFormHappyTests.CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + // no CR_LF
            "" + StreamingMultipartFormHappyTests.CR_LF +
            "value" + StreamingMultipartFormHappyTests.CR_LF +
            "-----2345--" + StreamingMultipartFormHappyTests.CR_LF).getBytes()), "Header didn't include a colon <<value>>");


        assertParseError(StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + StreamingMultipartFormHappyTests.CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + StreamingMultipartFormHappyTests.CR_LF +
            // no CR_LF
            "value" + StreamingMultipartFormHappyTests.CR_LF +
            "-----2345--" + StreamingMultipartFormHappyTests.CR_LF).getBytes()), "Header didn't include a colon <<value>>");
    }

    @Test
    public void failsIfContentsMissingFieldSeparator() throws Exception {
        String boundary = "---2345";

        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + StreamingMultipartFormHappyTests.CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + StreamingMultipartFormHappyTests.CR_LF +
            "" + StreamingMultipartFormHappyTests.CR_LF +
            "value" + // no CR_LF
            "-----2345--" + StreamingMultipartFormHappyTests.CR_LF).getBytes());

        form.next();
        // StreamingPart's content stream hasn't been closed
        assertParseErrorWrapsTokenNotFound(form, "Boundary must be proceeded by field separator, but didn't find it");
    }

    @Test
    public void failsIfContentsMissingFieldSeparatorAndHasReadToEndOfContent() throws Exception {
        String boundary = "---2345";

        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + StreamingMultipartFormHappyTests.CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + StreamingMultipartFormHappyTests.CR_LF +
            "" + StreamingMultipartFormHappyTests.CR_LF +
            "value" + // no CR_LF
            "-----2345--" + StreamingMultipartFormHappyTests.CR_LF).getBytes());

        StreamingPart StreamingPart = form.next();
        StreamingPart.getContentsAsString();
        assertParseErrorWrapsTokenNotFound(form, "Boundary must be proceeded by field separator, but didn't find it");
    }

    @Test
    public void failsIfClosingBoundaryIsMissingFieldSeparator() throws Exception {
        String boundary = "---2345";

        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + StreamingMultipartFormHappyTests.CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + StreamingMultipartFormHappyTests.CR_LF +
            "" + StreamingMultipartFormHappyTests.CR_LF +
            "value" + StreamingMultipartFormHappyTests.CR_LF +
            "-----2345--").getBytes()); // no CR_LF

        form.next();
        assertParseErrorWrapsTokenNotFound(form, "Stream terminator must be followed by field separator, but didn't find it");
    }

    @Test
    public void failsIfClosingBoundaryIsMissing() throws Exception {
        String boundary = "---2345";

        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + StreamingMultipartFormHappyTests.CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + StreamingMultipartFormHappyTests.CR_LF +
            "" + StreamingMultipartFormHappyTests.CR_LF +
            "value" + StreamingMultipartFormHappyTests.CR_LF +
            "-----2345" + StreamingMultipartFormHappyTests.CR_LF).getBytes());

        form.next();
        assertParseErrorWrapsTokenNotFound(form, "Reached end of stream before finding Token <<\r\n>>. Last 2 bytes read were <<>>");
    }

    @Test
    public void failsIfHeadingTooLong() throws Exception {
        String boundary = "---2345";

        char[] chars = new char[StreamingMultipartFormParts.HEADER_SIZE_MAX];
        Arrays.fill(chars, 'x');
        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .file("aFile", new String(chars), "application/octet-stream", "File contents here").build());

        assertParseErrorWrapsTokenNotFound(form, "Didn't find end of Token <<\r\n>> within 10240 bytes");
    }

    @Test
    public void failsIfTooManyHeadings() throws Exception {
        String boundary = "---2345";

        char[] chars = new char[1024];
        Arrays.fill(chars, 'x');
        Iterator<StreamingPart> form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
            .part("some contents",
                new Pair("Content-Disposition", asList(new Pair("form-data", null), new Pair("name", "fieldName"), new Pair("filename", "filename"))),
                new Pair("Content-Type", Arrays.asList(new Pair("text/plain", null))),
                new Pair("extra-1", Arrays.asList(new Pair(new String(chars), null))),
                new Pair("extra-2", Arrays.asList(new Pair(new String(chars), null))),
                new Pair("extra-3", Arrays.asList(new Pair(new String(chars), null))),
                new Pair("extra-4", Arrays.asList(new Pair(new String(chars), null))),
                new Pair("extra-5", Arrays.asList(new Pair(new String(chars), null))),
                new Pair("extra-6", Arrays.asList(new Pair(new String(chars), null))),
                new Pair("extra-7", Arrays.asList(new Pair(new String(chars), null))),
                new Pair("extra-8", Arrays.asList(new Pair(new String(chars), null))),
                new Pair("extra-9", Arrays.asList(new Pair(new String(chars), null))),
                new Pair("extra-10", Arrays.asList(new Pair(new String(chars, 0, 816), null))) // header section exactly 10240 bytes big!
            ).build());

        assertParseErrorWrapsTokenNotFound(form, "Didn't find end of Header section within 10240 bytes");
    }

    private void assertParseErrorWrapsTokenNotFound(Iterator<StreamingPart> form, String errorMessage) {
        try {
            form.hasNext();
            fail("Should have thrown a parse error");
        } catch (ParseError e) {
            assertThat(e.getCause().getClass(), equalTo(TokenNotFoundException.class));
            assertThat(e.getCause().getMessage(), equalTo(errorMessage));
        }
    }

    private void assertParseError(Iterator<StreamingPart> form, String errorMessage) {
        try {
            form.hasNext(); // will hit missing \r\n
            fail("Should have thrown a parse error");
        } catch (ParseError e) {
            assertThat(e.getMessage(), equalTo(errorMessage));
        }
    }


}
