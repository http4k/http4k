package org.http4k.contract.openapi.v3

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE
import com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.format.ConfigurableJackson
import org.http4k.format.Jackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import org.junit.jupiter.api.Test

internal class JacksonJsonNamingAnnotatedTest {

    @JsonNaming(UpperCamelCaseStrategy::class)
    data class Renamed(
        val renamedValue: String = "bob",
        val nullable: String? = "nullable",
        val user_name: String = "name"
    )

    data class Snake(val renamedValue: String = "bob")

    private val standard = JacksonJsonNamingAnnotated()

    @Test
    fun `finds value from object`() {
        assertThat(
            "nonNullable",
            standard(Renamed(), "RenamedValue"),
            equalTo(Field("bob", false, FieldMetadata.empty))
        )
        assertThat(
            "nonNullableNoRename",
            standard(Snake(), "renamedValue"),
            equalTo(Field("bob", false, FieldMetadata.empty))
        )
        assertThat("nullable", standard(Renamed(), "Nullable"), equalTo(Field("nullable", true, FieldMetadata.empty)))
    }

    @Test
    fun `throws on no field found`() {
        assertThat(
            "non existent",
            { JacksonJsonNamingAnnotated(Jackson)(Renamed(), "non existent") },
            throws<NoFieldFound>()
        )
    }

    object CustomJackson : ConfigurableJackson(
        KotlinModule.Builder().build()
            .asConfigurable()
            .withStandardMappings()
            .done().setPropertyNamingStrategy(SNAKE_CASE)
    )

    @Test
    fun `use custom Jackson naming strategy`() {
        assertThat(
            JacksonJsonNamingAnnotated(CustomJackson)(Snake(), "renamed_value"),
            equalTo(Field("bob", false, FieldMetadata.empty))
        )
    }
}
