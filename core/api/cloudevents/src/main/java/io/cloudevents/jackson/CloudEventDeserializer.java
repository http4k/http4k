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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.SpecVersion;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.BytesCloudEventData;
import io.cloudevents.rw.*;

import java.nio.charset.StandardCharsets;
import java.io.IOException;

/**
 * Jackson {@link com.fasterxml.jackson.databind.JsonDeserializer} for {@link CloudEvent}
 */
class CloudEventDeserializer extends StdDeserializer<CloudEvent> {
    private final boolean forceExtensionNameLowerCaseDeserialization;
    private final boolean forceIgnoreInvalidExtensionNameDeserialization;
    private final boolean disableDataContentTypeDefaulting;

    protected CloudEventDeserializer() {
        this(false, false, false);
    }

    protected CloudEventDeserializer(
        boolean forceExtensionNameLowerCaseDeserialization,
        boolean forceIgnoreInvalidExtensionNameDeserialization,
        boolean disableDataContentTypeDefaulting
    ) {
        super(CloudEvent.class);
        this.forceExtensionNameLowerCaseDeserialization = forceExtensionNameLowerCaseDeserialization;
        this.forceIgnoreInvalidExtensionNameDeserialization = forceIgnoreInvalidExtensionNameDeserialization;
        this.disableDataContentTypeDefaulting = disableDataContentTypeDefaulting;
    }

    private static class JsonMessage implements CloudEventReader {

        private final JsonParser p;
        private final ObjectNode node;
        private final boolean forceExtensionNameLowerCaseDeserialization;
        private final boolean forceIgnoreInvalidExtensionNameDeserialization;
        private final boolean disableDataContentTypeDefaulting;

        public JsonMessage(
            JsonParser p,
            ObjectNode node,
            boolean forceExtensionNameLowerCaseDeserialization,
            boolean forceIgnoreInvalidExtensionNameDeserialization,
            boolean disableDataContentTypeDefaulting
        ) {
            this.p = p;
            this.node = node;
            this.forceExtensionNameLowerCaseDeserialization = forceExtensionNameLowerCaseDeserialization;
            this.forceIgnoreInvalidExtensionNameDeserialization = forceIgnoreInvalidExtensionNameDeserialization;
            this.disableDataContentTypeDefaulting = disableDataContentTypeDefaulting;
        }

        @Override
        public <T extends CloudEventWriter<V>, V> V read(CloudEventWriterFactory<T, V> writerFactory, CloudEventDataMapper<? extends CloudEventData> mapper) throws CloudEventRWException, IllegalStateException {
            try {
                SpecVersion specVersion = SpecVersion.parse(getStringNode(this.node, this.p, "specversion"));
                CloudEventWriter<V> writer = writerFactory.create(specVersion);

                // TODO remove all the unnecessary code specversion aware

                // Read mandatory attributes
                for (String attr : specVersion.getMandatoryAttributes()) {
                    if (!"specversion".equals(attr)) {
                        writer.withContextAttribute(attr, getStringNode(this.node, this.p, attr));
                    }
                }

                // Parse datacontenttype if any
                String contentType = getOptionalStringNode(this.node, this.p, "datacontenttype");
                if (!this.disableDataContentTypeDefaulting && contentType == null && this.node.has("data")) {
                    contentType = "application/json";
                }
                if (contentType != null) {
                    writer.withContextAttribute("datacontenttype", contentType);
                }

                // Read optional attributes
                for (String attr : specVersion.getOptionalAttributes()) {
                    if (!"datacontentencoding".equals(attr)) { // Skip datacontentencoding, we need it later
                        String val = getOptionalStringNode(this.node, this.p, attr);
                        if (val != null) {
                            writer.withContextAttribute(attr, val);
                        }
                    }
                }

                CloudEventData data = null;

                // Now let's handle the data
                switch (specVersion) {
                    case V03:
                        boolean isBase64 = "base64".equals(getOptionalStringNode(this.node, this.p, "datacontentencoding"));
                        if (node.has("data")) {
                            if (isBase64) {
                                data = BytesCloudEventData.wrap(node.remove("data").binaryValue());
                            } else {
                                if (JsonFormat.dataIsJsonContentType(contentType)) {
                                    // This solution is quite bad, but i see no alternatives now.
                                    // Hopefully in future we can improve it
                                    data = JsonCloudEventData.wrap(node.remove("data"));
                                } else {
                                    JsonNode dataNode = node.remove("data");
                                    assertNodeType(dataNode, JsonNodeType.STRING, "data", "Because content type is not a json, only a string is accepted as data");
                                    data = BytesCloudEventData.wrap(dataNode.asText().getBytes(StandardCharsets.UTF_8));
                                }
                            }
                        }
                    case V1:
                        if (node.has("data_base64") && node.has("data")) {
                            throw MismatchedInputException.from(p, CloudEvent.class, "CloudEvent cannot have both 'data' and 'data_base64' fields");
                        }
                        if (node.has("data_base64")) {
                            data = BytesCloudEventData.wrap(node.remove("data_base64").binaryValue());
                        } else if (node.has("data")) {
                            if (JsonFormat.dataIsJsonContentType(contentType)) {
                                // This solution is quite bad, but i see no alternatives now.
                                // Hopefully in future we can improve it
                                data = JsonCloudEventData.wrap(node.remove("data"));
                            } else {
                                JsonNode dataNode = node.remove("data");
                                assertNodeType(dataNode, JsonNodeType.STRING, "data", "Because content type is not a json, only a string is accepted as data");
                                data = BytesCloudEventData.wrap(dataNode.asText().getBytes(StandardCharsets.UTF_8));
                            }
                        }
                }

                // Now let's process the extensions
                node.fields().forEachRemaining(entry -> {
                    String extensionName = entry.getKey();
                    if (this.forceExtensionNameLowerCaseDeserialization) {
                        extensionName = extensionName.toLowerCase();
                    }

                    if (this.shouldSkipExtensionName(extensionName)) {
                        return;
                    }

                    JsonNode extensionValue = entry.getValue();

                    switch (extensionValue.getNodeType()) {
                        case BOOLEAN:
                            writer.withContextAttribute(extensionName, extensionValue.booleanValue());
                            break;
                        case NUMBER:

                            final Number numericValue = extensionValue.numberValue();

                            // Only 'Int' values are supported by the specification

                            if (numericValue instanceof Integer){
                                writer.withContextAttribute(extensionName, (Integer) numericValue);
                            } else{
                                throw CloudEventRWException.newInvalidAttributeType(extensionName,numericValue);
                            }

                            break;
                        case STRING:
                            writer.withContextAttribute(extensionName, extensionValue.textValue());
                            break;
                        default:
                            writer.withContextAttribute(extensionName, extensionValue.toString());
                    }

                });

                if (data != null) {
                    return writer.end(mapper.map(data));
                }
                return writer.end();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(MismatchedInputException.from(this.p, CloudEvent.class, e.getMessage()));
            }
        }

        private String getStringNode(ObjectNode objNode, JsonParser p, String attributeName) throws JsonProcessingException {
            String val = getOptionalStringNode(objNode, p, attributeName);
            if (val == null) {
                throw MismatchedInputException.from(p, CloudEvent.class, "Missing mandatory " + attributeName + " attribute");
            }
            return val;
        }

        private String getOptionalStringNode(ObjectNode objNode, JsonParser p, String attributeName) throws JsonProcessingException {
            JsonNode unparsedAttribute = objNode.remove(attributeName);
            if (unparsedAttribute == null || unparsedAttribute instanceof NullNode) {
                return null;
            }
            assertNodeType(unparsedAttribute, JsonNodeType.STRING, attributeName, null);
            return unparsedAttribute.asText();
        }

        private void assertNodeType(JsonNode node, JsonNodeType type, String attributeName, String desc) throws JsonProcessingException {
            if (node.getNodeType() != type) {
                throw MismatchedInputException.from(
                    p,
                    CloudEvent.class,
                    "Wrong type " + node.getNodeType() + " for attribute " + attributeName + ", expecting " + type + (desc != null ? ". " + desc : "")
                );
            }
        }

        // ignore not valid extension name
        private boolean shouldSkipExtensionName(String extensionName) {
            return this.forceIgnoreInvalidExtensionNameDeserialization && !this.isValidExtensionName(extensionName);
        }

        /**
         * Validates the extension name as defined in  CloudEvents spec.
         *
         * @param name the extension name
         * @return true if extension name is valid, false otherwise
         * @see <a href="https://github.com/cloudevents/spec/blob/main/spec.md#attribute-naming-convention">attribute-naming-convention</a>
         */
        private boolean isValidExtensionName(String name) {
            for (int i = 0; i < name.length(); i++) {
                if (!isValidChar(name.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        private boolean isValidChar(char c) {
            return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
        }

    }

    @Override
    public CloudEvent deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        // In future we could eventually find a better solution avoiding this buffering step, but now this is the best option
        // Other sdk does the same in order to support all versions
        ObjectNode node = ctxt.readValue(p, ObjectNode.class);

        try {
            return new JsonMessage(p, node, this.forceExtensionNameLowerCaseDeserialization, this.forceIgnoreInvalidExtensionNameDeserialization, this.disableDataContentTypeDefaulting)
                .read(CloudEventBuilder::fromSpecVersion);
        } catch (RuntimeException e) {
            // Yeah this is bad but it's needed to support checked exceptions...
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw MismatchedInputException.from(p, CloudEvent.class, e.getMessage());
        }
    }
}
