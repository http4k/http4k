package org.http4k.contract.openapi.v3

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test

class FieldRetrievalTest {

    private val blowUp = FieldRetrieval { p1, p2 -> throw NoFieldFound(p2, p1) }

    private val result = Field("hello", true, FieldMetadata.empty)

    private val findIt = FieldRetrieval { _, _ -> result }

    data class Beany(val nonNullable: String = "hello", val aNullable: String? = "aNullable")

    @Test
    fun `bombs if can't find field anywhere`() {
        assertThat({ FieldRetrieval.compose(blowUp, blowUp)(Beany(), "foo") }, throws<NoFieldFound>())
    }

    @Test
    fun `field retrieval falls back if none found`() {
        assertThat(FieldRetrieval.compose(blowUp, findIt)(Beany(), "foo"), equalTo(Field("hello", true, FieldMetadata.empty)))
    }
}
