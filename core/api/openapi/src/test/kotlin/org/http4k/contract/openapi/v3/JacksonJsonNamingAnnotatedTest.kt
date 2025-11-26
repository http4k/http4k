package org.http4k.contract.openapi.v3

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.contract.jsonschema.v3.Field
import org.http4k.contract.jsonschema.v3.FieldMetadata
import org.http4k.contract.jsonschema.v3.JacksonJsonNamingAnnotated
import org.http4k.contract.jsonschema.v3.NoFieldFound
import org.http4k.format.ConfigurableJackson
import org.http4k.format.Jackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import org.junit.jupiter.api.Test
import tools.jackson.databind.PropertyNamingStrategies.SNAKE_CASE
import tools.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy
import tools.jackson.databind.annotation.JsonNaming
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

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
            .asConfigurable(
                JsonMapper.builder().deactivateDefaultTyping()
                    .changeDefaultPropertyInclusion { it.withValueInclusion(NON_NULL) }
                    .propertyNamingStrategy(SNAKE_CASE)
            )
            .withStandardMappings()
            .done()
    )

    @Test
    fun `use custom Jackson naming strategy`() {
        assertThat(
            JacksonJsonNamingAnnotated(CustomJackson)(Snake(), "renamed_value"),
            equalTo(Field("bob", false, FieldMetadata.empty))
        )
    }
}
