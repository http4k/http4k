package org.http4k.contract.openapi.v3

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.KotlinModule
import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory
import org.http4k.contract.openapi.OpenAPIJackson
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
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

enum class Enum1 {
     value
}

enum class Enum2 {
     value
}

data class ArbObject(
    val child: ArbObject2 = ArbObject2(),
    val list: List<ArbObject2> = listOf(ArbObject2(), ArbObject2()),
    val nestedList: List<List<ArbObject2>> = listOf(listOf(ArbObject2(), ArbObject2())),
    val nullableChild: ArbObject2? = ArbObject2(),
    val stringList: List<String> = listOf("hello", "goodbye"),
    val anyList: List<Any> = listOf("123", ArbObject2(), true, listOf(ArbObject2())),
    val enumVal: Foo? = Foo.value2
) : Generic

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

data class GenericListHolder(val value: List<Generic>)
data class MapHolder(val value: Map<Any, Any>)

data class JacksonFieldAnnotated(@JsonProperty("OTHERNAME") val uri: Uri = Uri.of("foobar"))

data class JacksonFieldWithMetadata(
    @JsonPropertyDescription("Field 1 description")
    val field1: String = "field1",
    val field2: String = "field2"
)

sealed class Sealed(val string: String)
object SealedChild : Sealed("child")

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
        approver.assertApproved(JsonPrimitives(), "foobar")
    }

    @Test
    fun `can provide custom prefix`(approver: Approver) {
        approver.assertApproved(InterfaceHolder(InterfaceImpl1()), null, "prefix")
    }

    @Test
    fun `can override definition id and it added to sub definitions`(approver: Approver) {
        approver.assertApproved(ArbObject(), "foobar")
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
                                "format" to "string format"
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
            "customPrefix"
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
        approver.assertApproved(MapHolder(mapOf(Foo.value1 to "value", LocalDate.of(1970, 1, 1) to "value")))
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
    fun `renders schema for top level generic list of enums`(approver: Approver) {
        approver.assertApproved(listOf(Enum1.value, Enum2.value))
    }

    @Test
    fun `renders schema for holder with generic list`(approver: Approver) {
        approver.assertApproved(GenericListHolder(listOf(ArbObject(), ArbObject2())))
    }

    @Test
    fun `renders schema for list of enums`(approver: Approver) {
        approver.assertApproved(listOf(Foo.value1, Foo.value2))
    }

    @Test
    fun `renders schema for enum`(approver: Approver) {
        approver.assertApproved(Foo.value1)
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
            MetaDataValueHolder(MyInt.of(1), JacksonFieldWithMetadata()), creator = autoJsonToJsonSchema(
                jackson
            )
        )
    }

    @Test
    fun `renders schema for object from sealed class`(approver: Approver) {
        approver.assertApproved(SealedChild)
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

    private fun autoJsonToJsonSchema(jackson: ConfigurableJackson) = AutoJsonToJsonSchema(
        jackson,
        FieldRetrieval.compose(
            SimpleLookup(
                metadataRetrievalStrategy =
                PrimitivesFieldMetadataRetrievalStrategy
                    .then(Values4kFieldMetadataRetrievalStrategy)
                    .then(JacksonFieldMetadataRetrievalStrategy)
            ),
            JacksonJsonPropertyAnnotated,
            JacksonJsonNamingAnnotated(Jackson)
        ),
        SchemaModelNamer.Full,
        "customPrefix"
    )
}
