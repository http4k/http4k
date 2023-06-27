package org.http4k.contract.jsonschema.v3

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.values.DoubleValue
import dev.forkhandles.values.FloatValue
import dev.forkhandles.values.InstantValue
import dev.forkhandles.values.IntValue
import dev.forkhandles.values.LocalDateValue
import dev.forkhandles.values.LongValue
import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.Value
import org.http4k.core.Uri
import org.http4k.core.UriValue
import org.http4k.contract.jsonschema.v3.FieldMetadata
import org.http4k.contract.jsonschema.v3.Values4kFieldMetadataRetrievalStrategy
import org.junit.jupiter.api.Test
import java.time.Instant.MAX
import java.time.LocalDate
import java.util.UUID

class Values4kFieldMetadataRetrievalStrategyTest {

    data class ValueHolder(val v: Value<*>)

    @Test
    fun `extract format from value type field`() {
        checkFormat(object : IntValue(1) {}, "int32")
        checkFormat(object : LongValue(1L) {}, "int64")
        checkFormat(object : DoubleValue(1.0) {}, "double")
        checkFormat(object : FloatValue(1.0f) {}, "float")
        checkFormat(object : UUIDValue(UUID.randomUUID()) {}, "uuid")
        checkFormat(object : UriValue(Uri.of("asd")) {}, "uri")
        checkFormat(object : LocalDateValue(LocalDate.MIN) {}, "date")
        checkFormat(object : InstantValue(MAX) {}, "date-time")
    }

    private fun checkFormat(target: Value<*>, s: String) {
        assertThat(
            Values4kFieldMetadataRetrievalStrategy(ValueHolder(target), "v"),
            equalTo(FieldMetadata(mapOf("format" to s)))
        )
    }
}
