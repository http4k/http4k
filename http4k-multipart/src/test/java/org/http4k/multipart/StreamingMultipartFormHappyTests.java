package org.http4k.multipart;

import kotlin.Pair;
import org.http4k.multipart.exceptions.AlreadyClosedException;
import org.http4k.multipart.part.StreamingPart;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StreamingMultipartFormHappyTests {

    public static final String CR_LF = "\r\n";

    @Test
    public void uploadEmptyContents() throws Exception {
        String boundary = "-----1234";
        Iterator<StreamingPart> form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary).build());

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void uploadEmptyFile() throws Exception {
        String boundary = "-----2345";
        Iterator<StreamingPart> form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
                .file("aFile", "", "doesnt/matter", "").build());

        assertFilePart(form, "aFile", "", "doesnt/matter", "");

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void hasNextIsIdempotent() throws Exception {
        String boundary = "-----2345";
        Iterator<StreamingPart> form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
                .file("aFile", "", "application/octet-stream", "")
                .file("anotherFile", "", "application/octet-stream", "").build());

        assertThereAreMoreParts(form);
        assertThereAreMoreParts(form);

        form.next();

        assertThereAreMoreParts(form);
        assertThereAreMoreParts(form);

        form.next();

        assertThereAreNoMoreParts(form);
        assertThereAreNoMoreParts(form);
    }

    @Test
    public void uploadEmptyField() throws Exception {
        String boundary = "-----3456";
        Iterator<StreamingPart> form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
                .field("aField", "").build());

        assertFieldPart(form, "aField", "");

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void uploadSmallFile() throws Exception {
        String boundary = "-----2345";
        Iterator<StreamingPart> form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
                .file("aFile", "file.name", "application/octet-stream", "File contents here").build());

        assertFilePart(form, "aFile", "file.name", "application/octet-stream", "File contents here");

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void uploadSmallFileAsAttachment() throws Exception {
        String boundary = "-----4567";
        Iterator<StreamingPart> form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
                .file("beforeFile", "before.txt", "application/json", "[]")
                .startMultipart("multipartFieldName", "7890")
                .attachment("during.txt", "plain/text", "Attachment contents here")
                .attachment("during2.txt", "plain/text", "More text here")
                .endMultipart()
                .file("afterFile", "after.txt", "application/json", "[]")
                .build());

        assertFilePart(form, "beforeFile", "before.txt", "application/json", "[]");
        assertFilePart(form, "multipartFieldName", "during.txt", "plain/text", "Attachment contents here");
        assertFilePart(form, "multipartFieldName", "during2.txt", "plain/text", "More text here");
        assertFilePart(form, "afterFile", "after.txt", "application/json", "[]");

        assertThereAreNoMoreParts(form);
    }


    @Test
    public void uploadSmallField() throws Exception {
        String boundary = "-----3456";
        Iterator<StreamingPart> form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
                .field("aField", "Here is the value of the field\n").build());

        assertFieldPart(form, "aField", "Here is the value of the field\n");

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void uploadMultipleFilesAndFields() throws Exception {
        String boundary = "-----1234";
        Iterator<StreamingPart> form = getMultipartFormParts(boundary,
                new ValidMultipartFormBuilder(boundary)
                        .file("file", "foo.tab", "text/whatever", "This is the content of the file\n")
                        .field("field", "fieldValue" + CR_LF + "with cr lf")
                        .field("multi", "value1")
                        .file("anotherFile", "BAR.tab", "text/something", "This is another file\n")
                        .field("multi", "value2")
                        .build());

        assertFilePart(form, "file", "foo.tab", "text/whatever", "This is the content of the file\n");
        assertFieldPart(form, "field", "fieldValue" + CR_LF + "with cr lf");
        assertFieldPart(form, "multi", "value1");
        assertFilePart(form, "anotherFile", "BAR.tab", "text/something", "This is another file\n");
        assertFieldPart(form, "multi", "value2");

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void uploadFieldsWithMultilineHeaders() throws Exception {
        String boundary = "-----1234";
        Iterator<StreamingPart> form = getMultipartFormParts(boundary,
                new ValidMultipartFormBuilder(boundary)
                        .rawPart(
                                "Content-Disposition: form-data; \r\n" +
                                        "\tname=\"field\"\r\n" +
                                        "\r\n" +
                                        "fieldValue")
                        .rawPart(
                                "Content-Disposition: form-data;\r\n" +
                                        "     name=\"multi\"\r\n" +
                                        "\r\n" +
                                        "value1")
                        .field("multi", "value2")
                        .build());

        assertFieldPart(form, "field", "fieldValue");
        assertFieldPart(form, "multi", "value1");
        assertFieldPart(form, "multi", "value2");

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void partsCanHaveLotsOfHeaders() throws Exception {
        String boundary = "-----1234";
        Iterator<StreamingPart> form = getMultipartFormParts(boundary,
                new ValidMultipartFormBuilder(boundary)
                        .part("This is the content of the file\n",
                                new Pair("Content-Disposition", asList(new Pair("form-data", null), new Pair("name", "fileFieldName"), new Pair("filename", "filename.txt"))),
                                new Pair("Content-Type", asList(new Pair("plain/text", null))),
                                new Pair("Some-header", asList(new Pair("some value", null))))
                        .part("This is the content of the field\n",
                                new Pair("Content-Disposition", asList(new Pair("form-data", null), new Pair("name", "fieldFieldName"))),
                                new Pair("Another-header", asList(new Pair("some-key", "some-value")))
                        )
                        .build());

        StreamingPart file = assertFilePart(form, "fileFieldName", "filename.txt", "plain/text", "This is the content of the file\n");

        Map<String, String> fileHeaders = file.getHeaders();
        assertThat(fileHeaders.size(), equalTo(3));
        assertThat(fileHeaders.get("Content-Disposition"), equalTo("form-data; name=\"fileFieldName\"; filename=\"filename.txt\""));
        assertThat(fileHeaders.get("Content-Type"), equalTo("plain/text"));
        assertThat(fileHeaders.get("Some-header"), equalTo("some value"));

        StreamingPart field = assertFieldPart(form, "fieldFieldName", "This is the content of the field\n");

        Map<String, String> fieldHeaders = field.getHeaders();
        assertThat(fieldHeaders.size(), equalTo(2));
        assertThat(fieldHeaders.get("Content-Disposition"), equalTo("form-data; name=\"fieldFieldName\""));
        assertThat(fieldHeaders.get("Another-header"), equalTo("some-key=\"some-value\""));

        assertThereAreNoMoreParts(form);
    }

    @Test
    public void closedPartsCannotBeReadFrom() throws Exception {
        String boundary = "-----2345";
        Iterator<StreamingPart> form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
                .file("aFile", "file.name", "application/octet-stream", "File contents here").build());

        StreamingPart file = form.next();

        //noinspection StatementWithEmptyBody
        while (file.inputStream.read() > 0) {
            // keep reading.
        }

        assertThat(file.inputStream.read(), equalTo(-1));
        file.inputStream.close();
        file.inputStream.close(); // can close multiple times
        try {
            int ignored = file.inputStream.read();
            fail("Should have complained that the StreamingPart has been closed " + ignored);
        } catch (AlreadyClosedException e) {
            // pass
        }
    }

    @Test
    public void readingPartsContentsAsStringClosesStream() throws Exception {
        String boundary = "-----2345";
        Iterator<StreamingPart> form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
                .file("aFile", "file.name", "application/octet-stream", "File contents here").build());

        StreamingPart file = form.next();
        file.getContentsAsString();

        try {
            int ignored = file.inputStream.read();
            fail("Should have complained that the StreamingPart has been closed " + ignored);
        } catch (AlreadyClosedException e) {
            // pass
        }

        file.inputStream.close(); // can close multiple times
    }

    @Test
    public void gettingNextPartClosesOldPart() throws Exception {
        String boundary = "-----2345";
        Iterator<StreamingPart> form = getMultipartFormParts(boundary, new ValidMultipartFormBuilder(boundary)
                .file("aFile", "file.name", "application/octet-stream", "File contents here")
                .file("anotherFile", "your.name", "application/octet-stream", "Different file contents here").build());

        StreamingPart file1 = form.next();

        StreamingPart file2 = form.next();

        assertThat(file1, not(equalTo(file2)));

        try {
            int ignored = file1.inputStream.read();
            fail("Should have complained that the StreamingPart has been closed " + ignored);
        } catch (AlreadyClosedException e) {
            // pass
        }

        file1.inputStream.close(); // can close multiple times

        assertThat(file2.getContentsAsString(), equalTo("Different file contents here"));
    }

    @Test
    public void canLoadComplexRealLifeSafariExample() throws Exception {
        Iterator<StreamingPart> parts = StreamingMultipartFormParts.Companion.parse(
                "----WebKitFormBoundary6LmirFeqsyCQRtbj".getBytes(StandardCharsets.UTF_8),
                new FileInputStream("examples/safari-example.multipart"),
                StandardCharsets.UTF_8
        ).iterator();

        assertFieldPart(parts, "articleType", "obituary");

        assertRealLifeFile(parts, "simple7bit.txt", "text/plain");
        assertRealLifeFile(parts, "starbucks.jpeg", "image/jpeg");
        assertRealLifeFile(parts, "utf8\uD83D\uDCA9.file", "application/octet-stream");
        assertRealLifeFile(parts, "utf8\uD83D\uDCA9.txt", "text/plain");

    }

    @Test
    public void canLoadComplexRealLifeChromeExample() throws Exception {
        Iterator<StreamingPart> parts = StreamingMultipartFormParts.Companion.parse(
                "----WebKitFormBoundaryft3FGhOMTYoOkCCc".getBytes(StandardCharsets.UTF_8),
                new FileInputStream("examples/chrome-example.multipart"),
                StandardCharsets.UTF_8
        ).iterator();

        assertFieldPart(parts, "articleType", "obituary");

        assertRealLifeFile(parts, "simple7bit.txt", "text/plain");
        assertRealLifeFile(parts, "starbucks.jpeg", "image/jpeg");
        assertRealLifeFile(parts, "utf8\uD83D\uDCA9.file", "application/octet-stream");
        assertRealLifeFile(parts, "utf8\uD83D\uDCA9.txt", "text/plain");

    }

    private void assertRealLifeFile(Iterator<StreamingPart> parts, String fileName, String contentType) throws IOException {
        StreamingPart file = parts.next();
        assertThat("field name", file.getFieldName(), equalTo("uploadManuscript"));
        assertThat("file name", file.getFileName(), equalTo(fileName));
        assertThat("content type", file.getContentType(), equalTo(contentType));
        assertPartIsNotField(file);
        compareStreamToFile(file);
    }

    public static void compareStreamToFile(StreamingPart file) throws IOException {
        InputStream formFile = file.inputStream;
        compareStreamToFile(formFile, file.getFileName());
    }

    public static void compareStreamToFile(InputStream actualSream, String fileName) throws IOException {
        InputStream original = new FileInputStream("examples/" + fileName);
        compareOneStreamToAnother(actualSream, original);
    }

    public static void compareOneStreamToAnother(InputStream actualStream, InputStream expectedStream) throws IOException {
        int index = 0;
        while (true) {
            int actual = actualStream.read();
            int expected = expectedStream.read();
            assertThat("index " + index, actual, equalTo(expected));
            index++;
            if (actual < 0) {
                break;
            }
        }
    }

    static Iterator<StreamingPart> getMultipartFormParts(String boundary, byte[] multipartFormContents) throws IOException {
        return getMultipartFormParts(boundary.getBytes(StandardCharsets.UTF_8), multipartFormContents, StandardCharsets.UTF_8);
    }

    static Iterator<StreamingPart> getMultipartFormParts(byte[] boundary, byte[] multipartFormContents, Charset encoding) throws IOException {
        InputStream multipartFormContentsStream = new ByteArrayInputStream(multipartFormContents);
        return StreamingMultipartFormParts.Companion.parse(boundary, multipartFormContentsStream, encoding).iterator();
    }

    static StreamingPart assertFilePart(Iterator<StreamingPart> form, String fieldName, String fileName, String contentType, String contents) throws IOException {
        return assertFilePart(form, fieldName, fileName, contentType, contents, StandardCharsets.UTF_8);
    }

    static StreamingPart assertFilePart(Iterator<StreamingPart> form, String fieldName, String fileName, String contentType, String contents, Charset encoding) throws IOException {
        assertThereAreMoreParts(form);
        StreamingPart file = form.next();
        assertThat("file name", file.getFileName(), equalTo(fileName));
        assertThat("content type", file.getContentType(), equalTo(contentType));
        assertPartIsNotField(file);
        assertPart(fieldName, contents, file, encoding);
        return file;
    }

    static StreamingPart assertFieldPart(Iterator<StreamingPart> form, String fieldName, String fieldValue) throws IOException {
        return assertFieldPart(form, fieldName, fieldValue, StandardCharsets.UTF_8);
    }

    static StreamingPart assertFieldPart(Iterator<StreamingPart> form, String fieldName, String fieldValue, Charset encoding) throws IOException {
        assertThereAreMoreParts(form);
        StreamingPart field = form.next();
        assertPartIsFormField(field);
        assertPart(fieldName, fieldValue, field, encoding);
        return field;
    }

    static void assertPart(String fieldName, String fieldValue, StreamingPart StreamingPart, Charset encoding) throws IOException {
        assertThat("field name", StreamingPart.getFieldName(), equalTo(fieldName));
        assertThat("contents", StreamingPart.getContentsAsString(encoding, 4096), equalTo(fieldValue));
    }

    static void assertThereAreNoMoreParts(Iterator<StreamingPart> form) {
        assertFalse("Too many parts", form.hasNext());
    }

    static void assertThereAreMoreParts(Iterator<StreamingPart> form) {
        assertTrue("Not enough parts", form.hasNext());
    }

    static void assertPartIsFormField(StreamingPart field) {
        assertTrue("the StreamingPart is a form field", field.isFormField());
    }

    static void assertPartIsNotField(StreamingPart file) {
        assertFalse("the StreamingPart is not a form field", file.isFormField());
    }
}
