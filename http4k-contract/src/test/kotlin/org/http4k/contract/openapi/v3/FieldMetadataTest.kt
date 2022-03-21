package org.http4k.contract.openapi.v3

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class FieldMetadataTest {

    @Test
    fun `can combine`() {
        val first = mapOf("foo" to "bar", "1" to 2)
        val second = mapOf("bar" to "foo")
        assertThat(FieldMetadata(first) + FieldMetadata(second), equalTo(FieldMetadata(first + second)))
    }
}
