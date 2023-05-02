package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.squareup.moshi.Moshi.Builder
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Moshi.auto
import org.http4k.format.StrictnessMode.FailOnUnknown
import org.http4k.lens.BiDiMapping
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

class MoshiAutoTest : AutoMarshallingJsonContract(Moshi) {

    override val expectedAutoMarshallingResult =
        """{"string":"hello","child":{"string":"world","numbers":[1],"bool":true},"numbers":[],"bool":false}"""

    @Test
    @Disabled("Currently doesn't work because of need for custom list adapters")
    fun `roundtrip list of arbitrary objects to and from body`() {
        val body = Body.auto<List<ArbObject>>().toLens()

        val expected = listOf(obj)
        val actual = body(Response(OK).with(body of expected))
        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `roundtrip array of arbitrary objects to and from JSON`() {
        val expected = arrayOf(obj)
        val asJsonString = Moshi.asFormatString(expected)
        val actual: Array<ArbObject> = Moshi.asA(asJsonString)
        assertThat(actual.toList(), equalTo(expected.toList()))
    }

    @Test
    fun `roundtrip list of arbitrary objects to and from JSON`() {
        val jsonString = Moshi.asJsonString(listOf(obj), List::class)
        val actual = Moshi.asA<Array<ArbObject>>(jsonString)
        val expected = arrayOf(obj)
        assertThat(actual.toList().toString(), actual.toList(), equalTo(expected.toList()))
    }

    @Test
    fun `read string to MoshiElement`() {
        val json =
            """{"string":"hello", "child":{"string":"world","numbers":[1, 1.2],"bool":true},"numbers":[],"bool":false}"""
        val expected = MoshiObject(
            mapOf(
                "string" to MoshiString("hello"),
                "child" to MoshiObject(
                    mapOf(
                        "string" to MoshiString("world"),
                        "numbers" to MoshiArray(
                            listOf(
                                MoshiInteger(1),
                                MoshiDecimal(1.2)
                            )
                        ),
                        "bool" to MoshiBoolean(true)
                    )
                ),
                "numbers" to MoshiArray(emptyList()),
                "bool" to MoshiBoolean(false)
            )
        )

        val element = with(Moshi) {
            json.asJsonObject()
        }

        assertThat(element, equalTo(expected))
    }

    @Test
    fun `write MoshiElement to string`() {
        val element = MoshiObject(
            mapOf(
                "string" to MoshiString("hello"),
                "child" to MoshiObject(
                    mapOf(
                        "string" to MoshiString("world"),
                        "numbers" to MoshiArray(
                            listOf(
                                MoshiInteger(1),
                                MoshiDecimal(1.2)
                            )
                        ),
                        "bool" to MoshiBoolean(true)
                    )
                ),
                "numbers" to MoshiArray(emptyList()),
                "bool" to MoshiBoolean(false)
            )
        )

        val json = with(Moshi) {
            element.asCompactJsonString()
        }

        assertThat(
            json,
            equalTo("""{"string":"hello","child":{"string":"world","numbers":[1,1.2],"bool":true},"numbers":[],"bool":false}""")
        )
    }

    @Test
    fun `convert arbObject to MoshiElement`() {
        assertThat(
            Moshi.asJsonObject(obj),
            equalTo(
                MoshiObject(
                    mapOf(
                        "string" to MoshiString("hello"),
                        "child" to MoshiObject(
                            mapOf(
                                "string" to MoshiString("world"),
                                "numbers" to MoshiArray(
                                    listOf(
                                        MoshiInteger(1)
                                    )
                                ),
                                "bool" to MoshiBoolean(true)
                            )
                        ),
                        "numbers" to MoshiArray(emptyList()),
                        "bool" to MoshiBoolean(false)
                    )
                )
            )
        )
    }

    @Test
    fun `convert MoshiElement to arbObject`() {
        val element = MoshiObject(
            mapOf(
                "string" to MoshiString("hello"),
                "child" to MoshiObject(
                    mapOf(
                        "string" to MoshiString("world"),
                        "numbers" to MoshiArray(
                            listOf(
                                MoshiInteger(1)
                            )
                        ),
                        "bool" to MoshiBoolean(true)
                    )
                ),
                "numbers" to MoshiArray(emptyList()),
                "bool" to MoshiBoolean(false)
            )
        )

        assertThat(
            Moshi.asA(element, ArbObject::class),
            equalTo(obj)
        )
    }

    @Test
    fun `custom moshi`() {
        val moshi = Moshi.custom {
            text(BiDiMapping({StringHolder(it)},{it.value}))
        }

        val value = StringHolder("stuff")
        assertThat(moshi.asFormatString(value), equalTo("\"stuff\""))
    }

    override fun strictMarshaller() = object : ConfigurableMoshi
        (Builder().asConfigurable().customise().done(), strictness = FailOnUnknown) {}

    override fun customMarshaller() = object : ConfigurableMoshi(Builder().asConfigurable().customise().done()) {}
    override fun customMarshallerProhibitStrings() = object : ConfigurableMoshi(
        Builder().asConfigurable().prohibitStrings().customise().done()
    ) {}
}

class MoshiJsonTest : JsonContract<MoshiNode>(Moshi) {
    override val prettyString = """{
    "hello": "world"
}"""

    @Test
    override fun `serializes object to json`() {
        j {
            val input = obj(
                "string" to string("value"),
                "double" to number(1.5),
                "long" to number(10L),
                "boolean" to boolean(true),
                "bigDec" to number(BigDecimal(1.2)),
                "bigInt" to number(BigInteger("12344")),
                "null" to nullNode(),
                "int" to number(2),
                "empty" to obj(),
                "array" to array(
                    listOf(
                        string(""),
                        number(123)
                    )
                ),
                "singletonArray" to array(obj("number" to number(123)))
            )
            val expected =
                """{"string":"value","double":1.5,"long":10,"boolean":true,"bigDec":1.2,"bigInt":12344,"int":2,"empty":{},"array":["",123],"singletonArray":[{"number":123}]}"""
            assertThat(compact(input), equalTo(expected))
        }
    }
}
