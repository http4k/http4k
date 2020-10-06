package org.http4k.contract.openapi.v3

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test

class SimpleLookupTest {

    data class Beany(
        val nonNullable: String = "hello",
        @JsonPropertyDescription("A field description")
        val withMeta: String = "withMeta",
        val aNullable: String? = "aNullable"
    )

    @Test
    fun `finds value from object`() {
        assertThat("nonNullable", SimpleLookup()(Beany(), "nonNullable"), equalTo(Field("hello", false, FieldMetadata.empty)))
        assertThat("aNullable", SimpleLookup()(Beany(), "aNullable"), equalTo(Field("aNullable", true, FieldMetadata.empty)))
        assertThat(
            "withMeta",
            SimpleLookup(metadataRetrievalStrategy = JacksonFieldMetadataRetrievalStrategy)(Beany(), "withMeta"),
            equalTo(Field("withMeta", false, FieldMetadata(description = "A field description")))
        )
    }

    @Test
    fun `throws on no field found`() {
        assertThat("non existent", { SimpleLookup()(Beany(), "non existent") }, throws<NoFieldFound>())
    }
}
