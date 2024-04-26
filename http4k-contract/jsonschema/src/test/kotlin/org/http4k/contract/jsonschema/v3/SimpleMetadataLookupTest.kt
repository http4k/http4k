package org.http4k.contract.jsonschema.v3

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType

class SimpleMetadataLookupTest {

    class KotlinBean
    class Blowup<T>(val inside: T)

    val simpleMetadataLookup =
        SimpleMetadataLookup(
            mapOf(
                KotlinBean::class.createType() to FieldMetadata(mapOf("description" to "A field description")),
                Blowup::class.createType(
                    listOf(KTypeProjection(KVariance.OUT, String::class.createType()))
                ) to FieldMetadata(mapOf("description" to "A field description"))
            )
        )

    @Test
    fun `finds value from object`() {
        assertThat(
            "exists",
            simpleMetadataLookup(KotlinBean()),
            equalTo(FieldMetadata(mapOf("description" to "A field description")))
        )
        assertThat(
            "does not exist",
            simpleMetadataLookup(object {}),
            equalTo(FieldMetadata())
        )
    }

    @Test
    fun `does not work with generics`() {
        assertThat("", simpleMetadataLookup(Blowup("inside")), equalTo(FieldMetadata()))
    }

}
