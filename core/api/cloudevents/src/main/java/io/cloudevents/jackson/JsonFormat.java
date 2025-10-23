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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.format.ContentType;
import io.cloudevents.core.format.EventDeserializationException;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.format.EventSerializationException;
import io.cloudevents.rw.CloudEventDataMapper;
import io.cloudevents.rw.CloudEventRWException;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Implementation of {@link EventFormat} for <a href="https://github.com/cloudevents/spec/blob/v1.0/json-format.md">JSON event format</a>
 * using Jackson. This format is resolvable with {@link io.cloudevents.core.provider.EventFormatProvider} using the content type {@link #CONTENT_TYPE}.
 * <p>
 * If you want to use the {@link CloudEvent} serializers/deserializers directly in your mapper, you can use {@link #getCloudEventJacksonModule()} or
 * {@link #getCloudEventJacksonModule(boolean, boolean)} to get a {@link SimpleModule} to register in your {@link ObjectMapper} instance.
 */
public final class JsonFormat implements EventFormat {

    /**
     * Content type associated with the JSON event format
     */
    public static final String CONTENT_TYPE = "application/cloudevents+json";
    /**
     * JSON Data Content Type Discriminator
     */
    private static final Pattern JSON_CONTENT_TYPE_PATTERN = Pattern.compile("^(application|text)\\/([a-zA-Z]+\\+)?json(;.*)*$");
    private final ObjectMapper mapper;
    private final JsonFormatOptions options;

    /**
     * Create a new instance of this class customizing the serialization configuration.
     *
     * @param forceDataBase64Serialization force json base64 encoding for data
     * @param forceStringSerialization     force string serialization for non json data field
     * @see #withForceJsonDataToBase64()
     * @see #withForceNonJsonDataToString()
     */
    public JsonFormat(boolean forceDataBase64Serialization, boolean forceStringSerialization) {
        this(
            JsonFormatOptions.builder()
                .forceDataBase64Serialization(forceDataBase64Serialization)
                .forceStringSerialization(forceStringSerialization)
                .build()
        );
    }

    /**
     * Create a new instance of this class customizing the serialization configuration.
     *
     * @param options json serialization / deserialization options
     */
    public JsonFormat(JsonFormatOptions options) {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(getCloudEventJacksonModule(options));
        this.options = options;
    }

    /**
     * Create a new instance of this class with default serialization configuration
     */
    public JsonFormat() {
        this(new JsonFormatOptions());
    }

    /**
     * @return a copy of this JsonFormat that serialize events with json data with Base64 encoding
     */
    public JsonFormat withForceJsonDataToBase64() {
        return new JsonFormat(
            JsonFormatOptions.builder()
                .forceDataBase64Serialization(true)
                .forceStringSerialization(this.options.isForceStringSerialization())
                .forceExtensionNameLowerCaseDeserialization(this.options.isForceExtensionNameLowerCaseDeserialization())
                .forceIgnoreInvalidExtensionNameDeserialization(this.options.isForceIgnoreInvalidExtensionNameDeserialization())
                .build()
        );
    }

    /**
     * @return a copy of this JsonFormat that serialize events with non-json data as string
     */
    public JsonFormat withForceNonJsonDataToString() {
        return new JsonFormat(
            JsonFormatOptions.builder()
                .forceDataBase64Serialization(this.options.isForceDataBase64Serialization())
                .forceStringSerialization(true)
                .forceExtensionNameLowerCaseDeserialization(this.options.isForceExtensionNameLowerCaseDeserialization())
                .forceIgnoreInvalidExtensionNameDeserialization(this.options.isForceIgnoreInvalidExtensionNameDeserialization())
                .build()
        );
    }

    /**
     * @return a copy of this JsonFormat that deserialize events with converting extension name lower case.
     */
    public JsonFormat withForceExtensionNameLowerCaseDeserialization() {
        return new JsonFormat(
            JsonFormatOptions.builder()
                .forceDataBase64Serialization(this.options.isForceDataBase64Serialization())
                .forceStringSerialization(this.options.isForceStringSerialization())
                .forceExtensionNameLowerCaseDeserialization(true)
                .forceIgnoreInvalidExtensionNameDeserialization(this.options.isForceIgnoreInvalidExtensionNameDeserialization())
                .build()
        );
    }

    /**
     * @return a copy of this JsonFormat that deserialize events with ignoring invalid extension name
     */
    public JsonFormat withForceIgnoreInvalidExtensionNameDeserialization() {
        return new JsonFormat(
            JsonFormatOptions.builder()
                .forceDataBase64Serialization(this.options.isForceDataBase64Serialization())
                .forceStringSerialization(this.options.isForceStringSerialization())
                .forceExtensionNameLowerCaseDeserialization(this.options.isForceExtensionNameLowerCaseDeserialization())
                .forceIgnoreInvalidExtensionNameDeserialization(true)
                .build()
        );
    }

    @Override
    public byte[] serialize(CloudEvent event) throws EventSerializationException {
        try {
            return mapper.writeValueAsBytes(event);
        } catch (JsonProcessingException e) {
            throw new EventSerializationException(e);
        }
    }

    @Override
    public CloudEvent deserialize(byte[] bytes) throws EventDeserializationException {
        try {
            return mapper.readValue(bytes, CloudEvent.class);
        } catch (IOException e) {
            throw new EventDeserializationException(e);
        }
    }

    @Override
    public CloudEvent deserialize(byte[] bytes, CloudEventDataMapper<? extends CloudEventData> mapper) throws EventDeserializationException {
        CloudEvent deserialized = this.deserialize(bytes);
        if (deserialized.getData() == null) {
            return deserialized;
        }
        try {
            return CloudEventBuilder.from(deserialized)
                .withData(mapper.map(deserialized.getData()))
                .build();
        } catch (CloudEventRWException e) {
            throw new EventDeserializationException(e);
        }
    }

    @Override
    public String serializedContentType() {
        return CONTENT_TYPE;
    }

    /**
     * @return a {@link SimpleModule} with {@link CloudEvent} serializer/deserializer configured using default values.
     */
    public static SimpleModule getCloudEventJacksonModule() {
        return getCloudEventJacksonModule(false, false);
    }

    /**
     * @param forceDataBase64Serialization force json base64 encoding for data
     * @param forceStringSerialization force string serialization for non json data field
     * @return a JacksonModule with CloudEvent serializer/deserializer customizing the data serialization.
     * @see #withForceJsonDataToBase64()
     * @see #withForceNonJsonDataToString()
     */
    public static SimpleModule getCloudEventJacksonModule(boolean forceDataBase64Serialization, boolean forceStringSerialization) {
        return getCloudEventJacksonModule(
            JsonFormatOptions.builder()
                .forceDataBase64Serialization(forceDataBase64Serialization)
                .forceStringSerialization(forceStringSerialization)
                .build()
        );
    }

    /**
     * @param options json serialization / deserialization options
     * @return a JacksonModule with CloudEvent serializer/deserializer customizing the data serialization.
     */
    public static SimpleModule getCloudEventJacksonModule(JsonFormatOptions options) {
        final SimpleModule ceModule = new SimpleModule("CloudEvent");
        ceModule.addSerializer(CloudEvent.class, new CloudEventSerializer(
            options.isForceDataBase64Serialization(), options.isForceStringSerialization()));
        ceModule.addDeserializer(CloudEvent.class, new CloudEventDeserializer(
            options.isForceExtensionNameLowerCaseDeserialization(), options.isForceIgnoreInvalidExtensionNameDeserialization(), options.isDataContentTypeDefaultingDisabled()));
        return ceModule;
    }

    static boolean dataIsJsonContentType(String contentType) {
        // If content type, spec states that we should assume is json
        return contentType == null || JSON_CONTENT_TYPE_PATTERN.matcher(contentType).matches();
    }
}
