package org.http4k.contract.openapi.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.ConfigurableJackson
import org.http4k.format.Jackson
import org.http4k.format.asConfigurable
import org.http4k.lens.BiDiMapping
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

data class ArbObject2(val uri: Uri = Uri.of("foobar"))

data class ArbObjectHolder(val inner: List<ArbObject2> = listOf(ArbObject2()))

data class ArbObject(
    val child: ArbObject2 = ArbObject2(),
    val list: List<ArbObject2> = listOf(ArbObject2(), ArbObject2()),
    val nestedList: List<List<ArbObject2>> = listOf(listOf(ArbObject2(), ArbObject2())),
    val nullableChild: ArbObject2? = ArbObject2(),
    val stringList: List<String> = listOf("hello", "goodbye"),
    val anyList: List<Any> = listOf("123", ArbObject2(), true, listOf(ArbObject2())),
    val enumVal: Foo = Foo.value2
)

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

data class MapHolder(val value: Map<Any, Any>)

@ExtendWith(JsonApprovalTest::class)
class AutoJsonToJsonSchemaTest {
    private val json = Jackson

    private val creator = AutoJsonToJsonSchema(json,
        FieldRetrieval.compose(SimpleLookup, JacksonAnnotated),
        SchemaModelNamer.Full,
        "customPrefix"
    )

    @Test
    fun `renders schema for various json primitives`(approver: Approver) {
        approver.assertApproved(JsonPrimitives(), null)
    }

    @Test
    fun `renders schema for nested arbitrary objects`(approver: Approver) {
        approver.assertApproved(ArbObject(), null)
    }

    @Test
    fun `renders schema for map field`(approver: Approver) {
        approver.assertApproved(MapHolder(
            mapOf(
                "key" to "value",
                "key2" to 123,
                "key3" to mapOf("inner" to ArbObject2())
            )
        ), null)
    }

    @Test
    fun `renders schema for non-string-keyed map field`(approver: Approver) {
        approver.assertApproved(MapHolder(mapOf(Foo.value1 to "value", LocalDate.EPOCH to "value")), null)
    }

    @Test
    fun `renders schema for freeform map field`(approver: Approver) {
        approver.assertApproved(MapHolder(emptyMap()), null)
    }

    @Test
    fun `renders schema for top level list`(approver: Approver) {
        approver.assertApproved(listOf(ArbObject()), null)
    }

    @Test
    fun `renders schema for when cannot find entry`(approver: Approver) {
        approver.assertApproved(JacksonFieldAnnotated(), null)
    }

    @Test
    fun `renders schema for custom json mapping`(approver: Approver) {
        val json = ConfigurableJackson(KotlinModule()
            .asConfigurable()
            .text(BiDiMapping({ i: String -> ArbObject2(Uri.of(i)) }, { i: ArbObject2 -> i.uri.toString() }))
            .done()
        )

        approver.assertApproved(Response(OK)
            .with(CONTENT_TYPE of APPLICATION_JSON)
            .body(Jackson.asJsonString(AutoJsonToJsonSchema(json).toSchema(ArbObjectHolder(), null))))
    }


    private fun Approver.assertApproved(obj: Any, name: String?) {
        assertApproved(Response(OK)
            .with(CONTENT_TYPE of APPLICATION_JSON)
            .body(Jackson.asJsonString(creator.toSchema(obj, name))))
    }
}

data class JacksonFieldAnnotated(@JsonProperty("OTHERNAME") val uri: Uri = Uri.of("foobar"))


