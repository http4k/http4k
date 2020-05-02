package org.http4k.contract.openapi.v3

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test

class SimpleLookupTest {

    data class Beany(val nonNullable: String = "hello", val aNullable: String? = "aNullable")

    @Test
    fun `finds value from object`() {
        assertThat("nonNullable", SimpleLookup()(Beany(), "nonNullable"), equalTo(Field("hello", false)))
        assertThat("aNullable", SimpleLookup()(Beany(), "aNullable"), equalTo(Field("aNullable", true)))
    }

    @Test
    fun `throws on no field found`() {
        assertThat("non existent", { SimpleLookup()(Beany(), "non existent") }, throws<NoFieldFound>())
    }
}

