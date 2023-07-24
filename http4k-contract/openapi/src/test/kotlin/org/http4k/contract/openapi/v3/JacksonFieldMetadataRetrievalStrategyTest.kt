package org.http4k.contract.openapi.v3

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.contract.jsonschema.v3.FieldMetadata
import org.http4k.contract.jsonschema.v3.JacksonFieldMetadataRetrievalStrategy
import org.junit.jupiter.api.Test

class JacksonFieldMetadataRetrievalStrategyTest {
    open class Base(@JsonPropertyDescription("Parent Field Description") val parentField: String = "parent")
    data class Model(
        @JsonPropertyDescription("Some Field Description") val someField: String = "hello",
        val fieldWithoutMeta: String = "world"
    ) : Base()

    @Test
    fun `extract description from annotated field`() {
        assertThat(
            JacksonFieldMetadataRetrievalStrategy(Model(), "someField"),
            equalTo(FieldMetadata(mapOf("description" to "Some Field Description")))
        )
    }

    @Test
    fun `extract description from annotated field in base class`() {
        assertThat(
            JacksonFieldMetadataRetrievalStrategy(Model(), "parentField"),
            equalTo(FieldMetadata(mapOf("description" to "Parent Field Description")))
        )
    }

    @Test
    fun `returns empty value when field is not annotated`() {
        assertThat(
            JacksonFieldMetadataRetrievalStrategy(Model(), "fieldWithoutMeta"),
            equalTo(FieldMetadata.empty)
        )
    }

    @Test
    fun `returns empty value when field is not found`() {
        assertThat(
            JacksonFieldMetadataRetrievalStrategy(Model(), "unknownField"),
            equalTo(FieldMetadata.empty)
        )
    }

    /**
     * Emulate a class annotated with `kotlinx.serialization`'s `@Serializable`.
     */
    class ModelWithMultipleConstructors {
        val describedField: String

        constructor(@JsonPropertyDescription("My description") describedField: String) {
            this.describedField = describedField
        }

        /**
         * This constructor is usually generated from `@Serializable`.
         */
        constructor(generatedField: Int, describedField: String, anotherGeneratedField: Any) {
            this.describedField = describedField
        }
    }

    @Test
    fun `extract description when multiple constructors are present`() {
        assertThat(
            JacksonFieldMetadataRetrievalStrategy(ModelWithMultipleConstructors(""), "describedField"),
            equalTo(FieldMetadata(mapOf("description" to "My description")))
        )
    }
}
