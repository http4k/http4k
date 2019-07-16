package org.http4k.contract.openapi.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategy.UpperCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test

internal class JacksonNamingConventionAnnotatedTest {

    @JsonNaming(UpperCamelCaseStrategy::class)
    data class Renamed(val renamedValue: String = "bob")

    @Test
    fun `finds value from object`() {
        assertThat("nonNullable", JacksonJsonPropertyAnnotated(Renamed(), "RenamedValue"), equalTo(Field("renamedValue", false)))
    }

    @Test
    fun `throws on no field found`() {
        assertThat("non existent", { JacksonJsonPropertyAnnotated(Renamed(), "non existent") }, throws<NoFieldFound>())
    }

}
