package org.http4k.contract.openapi.v3

import com.fasterxml.jackson.databind.PropertyNamingStrategy.UpperCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test

internal class JacksonJsonNamingAnnotatedTest {

    @JsonNaming(UpperCamelCaseStrategy::class)
    data class Renamed(val renamedValue: String = "bob")

    @Test
    fun `finds value from object`() {
        assertThat("nonNullable", JacksonJsonNamingAnnotated(Renamed(), "RenamedValue"), equalTo(Field("bob", false)))
    }

    @Test
    fun `throws on no field found`() {
        assertThat("non existent", { JacksonJsonNamingAnnotated(Renamed(), "non existent") }, throws<NoFieldFound>())
    }

}
