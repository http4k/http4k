package org.http4k.contract.jsonschema.v3

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.format.AutoMarshallingJson
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test

class AutoJsonToJsonSchemaJacksonTest : AutoJsonToJsonSchemaTest<JsonNode> {

    override val json = OpenAPIJackson

    override fun customJson(): AutoMarshallingJson<JsonNode> = object : ConfigurableJackson(
        KotlinModule.Builder().build()
            .asConfigurable()
            .withStandardMappings()
            .value(MyInt)
            .done()
            .deactivateDefaultTyping()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
            .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
            .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
    ) {}

    @Test
    fun `renders schema for data4k container and metadata`(approver: Approver) {
        val jackson = object : ConfigurableJackson(
            KotlinModule.Builder().build()
                .asConfigurable()
                .withStandardMappings()
                .value(MyInt)
                .done()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
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
