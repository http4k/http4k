/*
 * Copyright 2018-Present The CloudEvents Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.cloudevents.jackson;

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.core.CloudEventUtils;
import io.cloudevents.rw.CloudEventContextReader;
import io.cloudevents.rw.CloudEventContextWriter;
import io.cloudevents.rw.CloudEventRWException;
import org.jetbrains.annotations.NotNull;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.nio.charset.StandardCharsets;

/**
 * Jackson {@link SerializationContext} for {@link CloudEvent}
 */
class CloudEventSerializer extends StdSerializer<CloudEvent> {

    private final boolean forceDataBase64Serialization;
    private final boolean forceStringSerialization;

    protected CloudEventSerializer(boolean forceDataBase64Serialization, boolean forceStringSerialization) {
        super(CloudEvent.class);
        this.forceDataBase64Serialization = forceDataBase64Serialization;
        this.forceStringSerialization = forceStringSerialization;
    }


    private static class JsonContextWriter implements CloudEventContextWriter {

        private final JsonGenerator gen;

        public JsonContextWriter(JsonGenerator gen) {
            this.gen = gen;
        }

        @Override
        public CloudEventContextWriter withContextAttribute(@NotNull String name, @NotNull String value) throws CloudEventRWException {
            gen.writeName(name);
            gen.writeString(value);
            return this;
        }

        @Override
        public CloudEventContextWriter withContextAttribute(@NotNull String name, @NotNull Number value) throws CloudEventRWException {
            // Only Integer types are supported by the specification
            if (value instanceof Integer) {
                this.withContextAttribute(name, (Integer) value);
            } else {
                // Default to string representation for other numeric values
                this.withContextAttribute(name, value.toString());
            }
            return this;
        }

        @Override
        public CloudEventContextWriter withContextAttribute(@NotNull String name, @NotNull Integer value) throws CloudEventRWException {
            gen.writeName(name);
            gen.writeNumber(value);
            return this;
        }

        @Override
        public CloudEventContextWriter withContextAttribute(@NotNull String name, @NotNull Boolean value) throws CloudEventRWException {
            gen.writeName(name);
            gen.writeBoolean(value);
            return this;
        }
    }

    @Override
    public void serialize(CloudEvent value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
        gen.writeStartObject();
        gen.writeName("specversion");
        gen.writeString(value.getSpecVersion().toString());

        // Serialize attributes
        CloudEventContextReader contextReader = CloudEventUtils.toContextReader(value);
        JsonContextWriter contextWriter = new JsonContextWriter(gen);
        contextReader.readContext(contextWriter);

        // Serialize data
        if (value.getData() != null) {
            CloudEventData data = value.getData();
            if (data instanceof JsonCloudEventData) {
                gen.writeName("data");
                gen.writeEmbeddedObject(((JsonCloudEventData) data).getNode());
            } else {
                byte[] dataBytes = data.toBytes();
                String contentType = value.getDataContentType();
                if (shouldSerializeBase64(contentType)) {
                    switch (value.getSpecVersion()) {
                        case V03:
                            gen.writeName("datacontentencoding");
                            gen.writeString("base64");
                            gen.writeName("data");
                            gen.writeBinary(dataBytes);
                            break;
                        case V1:
                            gen.writeName("data_base64");
                            gen.writeBinary(dataBytes);
                            break;
                    }
                } else if (JsonFormat.dataIsJsonContentType(contentType)) {
                    // TODO really bad b/c it allocates stuff, is there another solution out there?
                    char[] dataAsString = new String(dataBytes, StandardCharsets.UTF_8).toCharArray();
                    gen.writeName("data");
                    gen.writeRawValue(dataAsString, 0, dataAsString.length);
                } else {
                    gen.writeName("data");
                    gen.writeUTF8String(dataBytes, 0, dataBytes.length);
                }
            }
        }
        gen.writeEndObject();
    }

    private boolean shouldSerializeBase64(String contentType) {
        if (JsonFormat.dataIsJsonContentType(contentType)) {
            return this.forceDataBase64Serialization;
        } else {
            return !this.forceStringSerialization;
        }
    }

}
