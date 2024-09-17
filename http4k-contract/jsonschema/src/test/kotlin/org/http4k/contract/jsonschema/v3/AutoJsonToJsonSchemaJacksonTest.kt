package org.http4k.contract.jsonschema.v3

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.format.ConfigurableJackson
import org.http4k.format.Jackson
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test

class AutoJsonToJsonSchemaJacksonTest : AutoJsonToJsonSchemaContract<JsonNode>() {

    override val json = Jackson

    @Suppress("DEPRECATION")
    @Test
    fun `renders schema for objects with metadata`(approver: Approver) {
        val jackson = object : ConfigurableJackson(
            KotlinModule.Builder().build()
                .asConfigurable()
                .withStandardMappings()
                .value(MyInt)
                .done()
                .deactivateDefaultTyping()
                .setSerializationInclusion(NON_NULL)
                .configure(FAIL_ON_NULL_FOR_PRIMITIVES, true)
                .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(FAIL_ON_IGNORED_PROPERTIES, false)
                .configure(USE_BIG_DECIMAL_FOR_FLOATS, true)
                .configure(USE_BIG_INTEGER_FOR_INTS, true)
                .configure(SORT_PROPERTIES_ALPHABETICALLY, true)
        ) {}

        approver.assertApproved(
            MetaDataValueHolder(MyInt.of(1), JacksonFieldWithMetadata()),
            creator = autoJsonToJsonSchema(jackson)
        )
    }

    @Test
    @Suppress("DEPRECATION")
    fun `renders schema for a data4k container with metadata`(approver: Approver) {
        val jackson = object : ConfigurableJackson(
            KotlinModule.Builder().build()
                .asConfigurable()
                .withStandardMappings()
                .value(MyInt)
                .done()
                .setSerializationInclusion(NON_NULL)
                .configure(SORT_PROPERTIES_ALPHABETICALLY, true)
        ) {}

        approver.assertApproved(
            Data4kContainer().apply {
                anInt = MyInt.of(123)
                anString = "helloworld"
            },
            creator = autoJsonToJsonSchema(
                jackson, strategy = PrimitivesFieldMetadataRetrievalStrategy
                    .then(Values4kFieldMetadataRetrievalStrategy)
                    .then(Data4kFieldMetadataRetrievalStrategy)
            )
        )
    }
}
