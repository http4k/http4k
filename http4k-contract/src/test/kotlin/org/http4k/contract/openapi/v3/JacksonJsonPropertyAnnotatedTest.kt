package org.http4k.contract.openapi.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test

class JacksonJsonPropertyAnnotatedTest {

    open class Foo(@JsonProperty("SUPERNEWNAME") val superValue: String = "bob")
    data class Beany(@JsonProperty("NEWNAME") val oldName: String = "hello") : Foo()

    @Test
    fun `finds value from object`() {
        assertThat("nonNullable", JacksonJsonPropertyAnnotated(Beany(), "NEWNAME"), equalTo(Field("hello", false, FieldMetadata.empty)))
    }

    @Test
    fun `finds value from superclass object`() {
        assertThat("superValue", JacksonJsonPropertyAnnotated(Beany(), "SUPERNEWNAME"), equalTo(Field("bob", false, FieldMetadata.empty)))
    }

    @Test
    fun `throws on no field found`() {
        assertThat("non existent", { JacksonJsonPropertyAnnotated(Beany(), "non existent") }, throws<NoFieldFound>())
    }
}
