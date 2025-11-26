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

public final class JsonFormatOptions {
    private final boolean forceDataBase64Serialization;
    private final boolean forceStringSerialization;
    private final boolean forceExtensionNameLowerCaseDeserialization;
    private final boolean forceIgnoreInvalidExtensionNameDeserialization;
    private final boolean disableDataContentTypeDefaulting;

    /**
     * Create a new instance of this class options the serialization / deserialization.
     */
    public JsonFormatOptions() {
        this(false, false, false, false, false);
    }

    JsonFormatOptions(
        boolean forceDataBase64Serialization,
        boolean forceStringSerialization,
        boolean forceExtensionNameLowerCaseDeserialization,
        boolean forceIgnoreInvalidExtensionNameDeserialization,
        boolean disableDataContentTypeDefaulting
    ) {
        this.forceDataBase64Serialization = forceDataBase64Serialization;
        this.forceStringSerialization = forceStringSerialization;
        this.forceExtensionNameLowerCaseDeserialization = forceExtensionNameLowerCaseDeserialization;
        this.forceIgnoreInvalidExtensionNameDeserialization = forceIgnoreInvalidExtensionNameDeserialization;
        this.disableDataContentTypeDefaulting = disableDataContentTypeDefaulting;
    }

    public static JsonFormatOptionsBuilder builder() {
        return new JsonFormatOptionsBuilder();
    }

    public boolean isForceDataBase64Serialization() {
        return this.forceDataBase64Serialization;
    }

    public boolean isForceStringSerialization() {
        return this.forceStringSerialization;
    }

    public boolean isForceExtensionNameLowerCaseDeserialization() {
        return this.forceExtensionNameLowerCaseDeserialization;
    }

    public boolean isForceIgnoreInvalidExtensionNameDeserialization() {
        return this.forceIgnoreInvalidExtensionNameDeserialization;
    }

    public boolean isDataContentTypeDefaultingDisabled() { return this.disableDataContentTypeDefaulting; }

    public static class JsonFormatOptionsBuilder {
        private boolean forceDataBase64Serialization = false;
        private boolean forceStringSerialization = false;
        private boolean forceExtensionNameLowerCaseDeserialization = false;
        private boolean forceIgnoreInvalidExtensionNameDeserialization = false;
        private boolean disableDataContentTypeDefaulting = false;

        public JsonFormatOptionsBuilder forceDataBase64Serialization(boolean forceDataBase64Serialization) {
            this.forceDataBase64Serialization = forceDataBase64Serialization;
            return this;
        }

        public JsonFormatOptionsBuilder forceStringSerialization(boolean forceStringSerialization) {
            this.forceStringSerialization = forceStringSerialization;
            return this;
        }

        public JsonFormatOptionsBuilder forceExtensionNameLowerCaseDeserialization(boolean forceExtensionNameLowerCaseDeserialization) {
            this.forceExtensionNameLowerCaseDeserialization = forceExtensionNameLowerCaseDeserialization;
            return this;
        }

        public JsonFormatOptionsBuilder forceIgnoreInvalidExtensionNameDeserialization(boolean forceIgnoreInvalidExtensionNameDeserialization) {
            this.forceIgnoreInvalidExtensionNameDeserialization = forceIgnoreInvalidExtensionNameDeserialization;
            return this;
        }

        public JsonFormatOptionsBuilder disableDataContentTypeDefaulting(boolean disableDataContentTypeDefaulting) {
            this.disableDataContentTypeDefaulting = disableDataContentTypeDefaulting;
            return this;
        }

        public JsonFormatOptions build() {
            return new JsonFormatOptions(
                this.forceDataBase64Serialization,
                this.forceStringSerialization,
                this.forceExtensionNameLowerCaseDeserialization,
                this.forceIgnoreInvalidExtensionNameDeserialization,
                this.disableDataContentTypeDefaulting
            );
        }
    }
}
