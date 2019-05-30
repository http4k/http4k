package org.http4k.contract.openapi.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test

class JacksonAnnotatedTest {

    data class Beany(@JsonProperty("NEWNAME") val oldName: String = "hello")

    @Test
    fun `finds value from object`() {
        assertThat("nonNullable", JacksonAnnotated(Beany(), "NEWNAME"), equalTo(Field("hello", false)))
    }

    @Test
    fun `throws on no field found`() {
        assertThat("non existent", { JacksonAnnotated(Beany(), "non existent") }, throws<NoFieldFound>())
    }
}

