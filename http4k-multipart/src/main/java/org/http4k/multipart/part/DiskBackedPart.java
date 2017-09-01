package org.http4k.multipart.part;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystemException;

public class DiskBackedPart extends Part {

    private final File theFile;

    public DiskBackedPart(PartMetaData part, File theFile) {
        super(part.fieldName, part.formField, part.contentType, part.fileName, part.headers, (int) theFile.length());
        this.theFile = theFile;
    }

    public InputStream getNewInputStream() throws IOException {
        return new FileInputStream(theFile);
    }

    @Override public boolean isInMemory() {
        return false;
    }

    @Override public byte[] getBytes() {
        throw new IllegalStateException("Cannot get bytes from a DiskBacked Part. Check with isInMemory()");
    }

    @Override public String getString() {
        throw new IllegalStateException("Cannot get bytes from a DiskBacked Part. Check with isInMemory()");
    }

    public void close() throws IOException {
        if (!theFile.delete()) {
            throw new FileSystemException("Failed to delete file");
        }
    }
}
