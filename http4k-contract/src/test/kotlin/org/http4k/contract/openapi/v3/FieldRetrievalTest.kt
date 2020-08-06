package org.http4k.contract.openapi.v3

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test

class FieldRetrievalTest {

    private val blowUp = object : FieldRetrieval {
        override fun invoke(p1: Any, p2: String) = throw NoFieldFound(p2, p1)
    }

    private val result = Field("hello", true, FieldMetadata.empty)

    private val findIt = object : FieldRetrieval {
        override fun invoke(p1: Any, p2: String): Field = result
    }

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
