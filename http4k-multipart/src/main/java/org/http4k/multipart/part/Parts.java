package org.http4k.multipart.part;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Parts implements AutoCloseable {
    public final Map<String, List<Part>> partMap;

    public Parts(Map<String, List<Part>> partMap) {
        this.partMap = Collections.unmodifiableMap(partMap);
    }

    @Override public void close() throws IOException {
        for (List<Part> parts : partMap.values()) {
            for (Part part : parts) {
                part.close();
            }
        }

    }
}
