package org.http4k.contract.jsonschema.v3

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import dev.forkhandles.data.MapDataContainer
import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory
import org.http4k.contract.jsonschema.v3.Foo.value1
import org.http4k.contract.jsonschema.v3.Foo.value2
import org.http4k.contract.jsonschema.v3.Data4kJsonSchemaMeta.default
import org.http4k.contract.jsonschema.v3.Data4kJsonSchemaMeta.exclusiveMinimum
import org.http4k.contract.jsonschema.v3.Data4kJsonSchemaMeta.format
import org.http4k.contract.jsonschema.v3.Data4kJsonSchemaMeta.maxLength
import org.http4k.contract.jsonschema.v3.SchemaModelNamer.Companion.Canonical
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.ConfigurableJackson
import org.http4k.format.Jackson
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import org.http4k.lens.BiDiMapping
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

interface Generic

data class RecursiveObject(val children: List<RecursiveObject> = emptyList())

data class ArbObject2(val uri: Uri = Uri.of("foobar")) : Generic
data class ArbObject3(val str: String = "stringValue", val num: Int = 1) : Generic

data class ArbObjectHolder(val inner: List<ArbObject2> = listOf(ArbObject2()))

enum class Enum1 : Generic {
    value1_1, value1_2
}

enum class Enum2 : Generic {
    value2_1, value2_2
}

data class ArbObject(
    val child: ArbObject2 = ArbObject2(),
    val list: List<ArbObject2> = listOf(ArbObject2(), ArbObject2()),
    val nestedList: List<List<ArbObject2>> = listOf(listOf(ArbObject2(), ArbObject2())),
    val nullableChild: ArbObject2? = ArbObject2(),
    val stringList: List<String> = listOf("hello", "goodbye"),
    val anyList: List<Any> = listOf("123", ArbObject2(), true, listOf(ArbObject2())),
    val enumVal: Foo? = value2
) : Generic

class Data4kContainer : MapDataContainer() {
    var anInt by required(MyInt, format of "foobar", default of 123)
    var anString by required<String>(maxLength of 12, exclusiveMinimum of true)
}

data class ArbObjectWithInnerClasses(
    val inner: Inner = Inner(),
    val enum: FooEnum = FooEnum.bar,
) : Generic {
    data class Inner(val name: String = "name")
    enum class FooEnum {
        bar
    }
}

data class JsonPrimitives(
    val string: String = "string",
    val boolean: Boolean = false,
    val int: Int = Int.MAX_VALUE,
    val long: Long = Long.MIN_VALUE,
    val double: Double = 9.9999999999,
    val float: Float = -9.9999999999f,
    val bigInt: BigInteger = BigInteger("" + Long.MAX_VALUE + "" + Long.MAX_VALUE),
    val bigDecimal: BigDecimal = BigDecimal("" + Double.MAX_VALUE + "" + 111)
)

enum class Foo {
    value1, value2
}

data class Nulls(val f1: String? = null, val f2: String? = null)

data class EnumListHolder<T : Enum<T>>(val value: List<T>)

data class GenericListHolder(val value: List<Generic>)
data class GenericHolder<T>(val value: T)

data class MapHolder(val value: Map<Any, Any>)

data class JacksonFieldAnnotated(@JsonProperty("OTHERNAME") val uri: Uri = Uri.of("foobar"))

data class JacksonFieldWithMetadata(
    @JsonPropertyDescription("Field 1 description")
    val field1: String = "field1",
    val field2: String = "field2"
)

sealed class Sealed(val string: String)
data object SealedChild : Sealed("child")

interface Interface
data class InterfaceImpl1(val a: String = "a") : Interface
data class InterfaceHolder(val i: Interface)

class MyInt private constructor(value: Int) : IntValue(value) {
    companion object : IntValueFactory<MyInt>(::MyInt)
}

data class MetaDataValueHolder(val i: MyInt, val j: JacksonFieldWithMetadata)

@ExtendWith(JsonApprovalTest::class)
class AutoJsonToJsonSchemaTest {
    private val json = OpenAPIJackson

    @Test
    fun `can override definition id`(approver: Approver) {
        approver.assertApproved(JsonPrimitives(), name = "foobar")
    }

    @Test
    fun `can provide custom prefix`(approver: Approver) {
        approver.assertApproved(InterfaceHolder(InterfaceImpl1()), null, prefix = "prefix")
    }

    @Test
    fun `can provide custom prefix for inner classes`(approver: Approver) {
        approver.assertApproved(
            ArbObjectWithInnerClasses(),
            prefix = "prefix",
            creator = autoJsonToJsonSchema(json, Canonical)
        )
    }

    @Test
    fun `can override definition id and it added to sub definitions`(approver: Approver) {
        approver.assertApproved(ArbObject(), name = "foobar")
    }

    @Test
    fun `can override definition id for a map`(approver: Approver) {
        approver.assertApproved(
            MapHolder(
                mapOf(
                    "key" to "value",
                    "key2" to 123,
                    "key3" to mapOf("inner" to ArbObject2())
                )
            ), "foobar"
        )
    }

    @Test
    fun `can write extra properties to map`(approver: Approver) {
        val creator = AutoJsonToJsonSchema(
            json,
            { _, name ->
                if (name == "str") {
                    Field(
                        "hello",
                        false,
                        FieldMetadata(
                            mapOf(
                                "key" to "string",
                                "description" to "string description",
                                "format" to "string format",
                                "a-x-thing" to "some special value"
                            )
                        )
                    )
                } else {
                    Field(
                        123,
                        false,
                        FieldMetadata(
                            mapOf(
                                "key" to 123,
                                "description" to "int description",
                                "format" to "another format"
                            )
                        )
                    )
                }
            },
            SchemaModelNamer.Full,
            "locationPrefix"
        )

        approver.assertApproved(
            Response(OK)
                .with(CONTENT_TYPE of APPLICATION_JSON)
                .body(Jackson.asFormatString(creator.toSchema(ArbObject3(), refModelNamePrefix = null)))
        )
    }

    @Test
    fun `can add extra properties to a root component`(approver: Approver) {
        val creator = AutoJsonToJsonSchema(
            json,
            metadataRetrieval = { obj ->
                if (obj is ArbObject3) {
                    FieldMetadata(
                        mapOf(
                            "key" to "arb",
                            "description" to "arb desc",
                            "additionalProperties" to false
                        )
                    )
                } else {
                    FieldMetadata()
                }
            },
            modelNamer = SchemaModelNamer.Full,
            refLocationPrefix = "locationPrefix"
        )

        approver.assertApproved(
            Response(OK)
                .with(CONTENT_TYPE of APPLICATION_JSON)
                .body(Jackson.asFormatString(creator.toSchema(ArbObject3(), refModelNamePrefix = null)))
        )
    }

    @Test
    fun `can override definition id for a raw map`(approver: Approver) {
        approver.assertApproved(
            mapOf(
                "key" to "value",
                "key2" to 123,
                "key3" to mapOf("inner" to ArbObject2())
            ), "foobar"
        )
    }

    @Test
    fun `renders schema for various json primitives`(approver: Approver) {
        approver.assertApproved(JsonPrimitives())
    }

    @Test
    fun `renders schema for nested arbitrary objects`(approver: Approver) {
        approver.assertApproved(ArbObject())
    }

    @Test
    fun `renders schema for nested arbitrary objects with prefix`(approver: Approver) {
        approver.assertApproved(ArbObject(), prefix = "prefix")
    }

    @Test
    fun `renders schema for objet with all optional fields`(approver: Approver) {
        approver.assertApproved(Nulls("foo", "bar"))
    }

    @Test
    fun `renders schema for recursive objects`(approver: Approver) {
        approver.assertApproved(RecursiveObject(listOf(RecursiveObject())))
    }

    @Test
    fun `renders schema for map field`(approver: Approver) {
        approver.assertApproved(
            MapHolder(
                mapOf(
                    "key" to "value",
                    "key2" to 123,
                    "key3" to mapOf("inner" to ArbObject2()),
                    "key4" to ArbObject2()
                )
            )
        )
    }

    @Test
    fun `renders schema for raw map field`(approver: Approver) {
        approver.assertApproved(
            mapOf(
                "key" to "value",
                "key2" to 123,
                "key3" to mapOf("inner" to ArbObject2()),
                "key4" to ArbObject2(),
                "key5" to listOf(ArbObject2(), ArbObject()),
                "key6" to listOf(1, 2),
                "key7" to GenericListHolder(listOf(ArbObject2(), ArbObject()))
            )
        )
    }

    @Test
    fun `renders schema for non-string-keyed map field`(approver: Approver) {
        approver.assertApproved(MapHolder(mapOf(value1 to "value", LocalDate.of(1970, 1, 1) to "value")))
    }

    @Test
    fun `renders schema for freeform map field`(approver: Approver) {
        approver.assertApproved(MapHolder(emptyMap()))
    }

    @Test
    fun `renders schema for top level list`(approver: Approver) {
        approver.assertApproved(listOf(ArbObject()))
    }

    @Test
    fun `renders schema for top level generic list`(approver: Approver) {
        approver.assertApproved(listOf(ArbObject(), ArbObject2()))
    }

    @Test
    fun `renders schema for different enum types`(approver: Approver) {
        approver.assertApproved(
            listOf(
                GenericHolder(Enum1.value1_1),
                GenericHolder(Enum2.value2_1),
                GenericHolder(Enum1.value1_2)
            )
        )
    }

    @Test
    fun `renders schema for holder with generic list`(approver: Approver) {
        approver.assertApproved(GenericListHolder(listOf(ArbObject(), ArbObject2())))
    }

    @Test
    fun `renders schema for list of enums`(approver: Approver) {
        approver.assertApproved(listOf(value1, value2))
    }

    @Test
    fun `renders schema for nested list of enums`(approver: Approver) {
        approver.assertApproved(EnumListHolder(listOf(value1, value2)))
    }

    @Test
    fun `renders schema for enum`(approver: Approver) {
        approver.assertApproved(value1)
    }

    @Test
    fun `renders schema for when cannot find entry`(approver: Approver) {
        approver.assertApproved(JacksonFieldAnnotated())
    }

    @Test
    fun `renders schema for custom json mapping`(approver: Approver) {
        val json = ConfigurableJackson(
            KotlinModule.Builder().build()
                .asConfigurable()
                .text(BiDiMapping({ i: String -> ArbObject2(Uri.of(i)) }, { i: ArbObject2 -> i.uri.toString() }))
                .done()
        )

        approver.assertApproved(
            Response(OK)
                .with(CONTENT_TYPE of APPLICATION_JSON)
                .body(
                    Jackson.asFormatString(
                        AutoJsonToJsonSchema(json).toSchema(
                            ArbObjectHolder(),
                            refModelNamePrefix = null
                        )
                    )
                )
        )
    }

    @Test
    fun `renders schema for field with description`(approver: Approver) {
        approver.assertApproved(JacksonFieldWithMetadata())
    }

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
        ) {}

        approver.assertApproved(
            MetaDataValueHolder(MyInt.of(1), JacksonFieldWithMetadata()),
            creator = autoJsonToJsonSchema(jackson)
        )
    }

    @Test
    fun `renders schema for object from sealed class`(approver: Approver) {
        approver.assertApproved(SealedChild)
    }

    @Test
    fun `renders schema for data4k container and metadata`(approver: Approver) {
        val jackson = object : ConfigurableJackson(
            KotlinModule.Builder().build()
                .asConfigurable()
                .withStandardMappings()
                .value(MyInt)
                .done()
                .setSerializationInclusion(NON_NULL)
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

    private fun Approver.assertApproved(
        obj: Any,
        name: String? = null,
        prefix: String? = null,
        creator: AutoJsonToJsonSchema<JsonNode> = autoJsonToJsonSchema(json)
    ) {
        assertApproved(
            Response(OK)
                .with(CONTENT_TYPE of APPLICATION_JSON)
                .body(Jackson.asFormatString(creator.toSchema(obj, name, prefix)))
        )
    }

    private fun autoJsonToJsonSchema(
        jackson: ConfigurableJackson,
        schemaModelNamer: SchemaModelNamer = SchemaModelNamer.Full,
        strategy: FieldMetadataRetrievalStrategy = PrimitivesFieldMetadataRetrievalStrategy
            .then(Values4kFieldMetadataRetrievalStrategy)
            .then(JacksonFieldMetadataRetrievalStrategy)
    ) = AutoJsonToJsonSchema(
        jackson,
        FieldRetrieval.compose(
            SimpleLookup(metadataRetrievalStrategy = strategy),
            JacksonJsonPropertyAnnotated,
            JacksonJsonNamingAnnotated(Jackson)
        ),
        schemaModelNamer,
        "locationPrefix"
    )
}

private fun standardConfig(
    configFn: AutoMappingConfiguration<ObjectMapper>.() -> AutoMappingConfiguration<ObjectMapper>
) = KotlinModule.Builder().build()
    .asConfigurable()
    .withStandardMappings()
    .let(configFn)
    .done()
    .deactivateDefaultTyping()
    .setSerializationInclusion(NON_NULL)
    .configure(FAIL_ON_NULL_FOR_PRIMITIVES, true)
    .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(FAIL_ON_IGNORED_PROPERTIES, false)
    .configure(USE_BIG_DECIMAL_FOR_FLOATS, true)
    .configure(USE_BIG_INTEGER_FOR_INTS, true)

object OpenAPIJackson : ConfigurableJackson(standardConfig { this }) {
    fun custom(configFn: AutoMappingConfiguration<ObjectMapper>.() -> AutoMappingConfiguration<ObjectMapper>) =
        ConfigurableJackson(standardConfig(configFn))
}
