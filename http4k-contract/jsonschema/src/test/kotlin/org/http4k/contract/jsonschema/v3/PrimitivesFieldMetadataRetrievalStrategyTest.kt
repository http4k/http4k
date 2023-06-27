package org.http4k.contract.jsonschema.v3

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Uri
import org.http4k.contract.jsonschema.v3.FieldMetadata
import org.http4k.contract.jsonschema.v3.PrimitivesFieldMetadataRetrievalStrategy
import org.junit.jupiter.api.Test
import java.time.Instant.MAX
import java.time.LocalDate
import java.util.UUID

class PrimitivesFieldMetadataRetrievalStrategyTest {

    data class ValueHolder(val v: Any)

    @Test
    fun `extract format from value type field`() {
        checkFormat(1, "int32")
        checkFormat(1L, "int64")
        checkFormat(1.0, "double")
        checkFormat(1.0f, "float")
        checkFormat(UUID.randomUUID(), "uuid")
        checkFormat(Uri.of(""), "uri")
        checkFormat(LocalDate.MIN, "date")
        checkFormat(MAX, "date-time")
    }

    private fun checkFormat(target: Any, s: String) {
        assertThat(
            PrimitivesFieldMetadataRetrievalStrategy(ValueHolder(target), "v"),
            equalTo(FieldMetadata(mapOf("format" to s)))
        )
    }
}
