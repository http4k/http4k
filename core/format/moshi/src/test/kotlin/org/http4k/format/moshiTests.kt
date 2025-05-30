package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.startsWith
import com.squareup.moshi.Moshi.Builder
import dev.forkhandles.data.MoshiNodeDataContainer
import dev.forkhandles.values.AbstractValue
import dev.forkhandles.values.BooleanValueFactory
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Moshi.auto
import org.http4k.format.Moshi.json
import org.http4k.format.Moshi.obj
import org.http4k.format.StrictnessMode.FailOnUnknown
import org.http4k.hamkrest.hasBody
import org.http4k.lens.BiDiMapping
import org.http4k.websocket.WsMessage
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
    override fun `automarshalling failure has expected message`() {
        assertThat(runCatching { Moshi.autoBody<ArbObject>().toLens()(invalidArbObjectRequest) }
            .exceptionOrNull()!!.message!!, startsWith("Required value 'string' missing at \$"))
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
            mutableMapOf(
                "string" to MoshiString("hello"),
                "child" to MoshiObject(
                    mutableMapOf(
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

        val element = with(Moshi) { json.asJsonObject() }

        assertThat(element, equalTo(expected))
        assertThat(Moshi.asA<MoshiObject>(json), equalTo(expected))
    }

    @Test
    fun `write MoshiElement to string`() {
        val element = MoshiObject(
            mutableMapOf(
                "string" to MoshiString("hello"),
                "child" to MoshiObject(
                    mutableMapOf(
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

        val expected =
            """{"string":"hello","child":{"string":"world","numbers":[1,1.2],"bool":true},"numbers":[],"bool":false}"""
        assertThat(
            with(Moshi) { element.asCompactJsonString() },
            equalTo(expected)
        )

        assertThat(Moshi.asFormatString(element), equalTo(expected))
    }

    @Test
    fun `convert arbObject to MoshiElement`() {
        assertThat(
            Moshi.asJsonObject(obj),
            equalTo(
                MoshiObject(
                    mutableMapOf(
                        "string" to MoshiString("hello"),
                        "child" to MoshiObject(
                            mutableMapOf(
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
            mutableMapOf(
                "string" to MoshiString("hello"),
                "child" to MoshiObject(
                    mutableMapOf(
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
            text(BiDiMapping({ StringHolder(it) }, { it.value }))
        }

        val value = StringHolder("stuff")
        assertThat(moshi.asFormatString(value), equalTo("\"stuff\""))
    }

    @Test
    fun `throws on mapped value class when we specifically prohibit it`() {
        val marshaller =
            ConfigurableMoshi(standardConfig()
                .apply {
                    value(MyValue)
                    value(MyOtherValue)
                }
                .customise()
                .add(ProhibitUnknownValuesAdapter)
            )

        assertThat(marshaller.asFormatString(MyValue.of("hello")), equalTo(""""hello""""))
        assertThat(marshaller.asFormatString(MyOtherValue.of("world")), equalTo(""""world""""))
        assertThrows<Exception> { marshaller.asFormatString(UnknownValueType.of("unknown")) }
    }

    @Test
    fun `roundtrip list of arbitrary objects to and with BiDi lens`() {
        val lens = Moshi.asBiDiMapping<ArbObject>()
        assertThat(lens(lens(obj)), equalTo(obj))
    }

    override fun strictMarshaller() =
        object : ConfigurableMoshi(Builder().asConfigurable().customise(), strictness = FailOnUnknown) {}

    override fun customMarshaller() =
        object : ConfigurableMoshi(Builder().asConfigurable().customise()) {}

    override fun customMarshallerProhibitStrings() =
        object : ConfigurableMoshi(Builder().asConfigurable().prohibitStrings().customise()) {}
}

class MoshiJsonTest : JsonContract<MoshiNode>(Moshi) {
    override val prettyString = """{
    "hello": "world"
}"""

    @Test
    fun `handles long values correctly`() {
        val input = Long.MAX_VALUE
        val json = Moshi.asFormatString(input)
        assertThat(Moshi.parse(json), equalTo(MoshiLong(input)))
        assertThat(Moshi.asA<Long>(json), equalTo(input))
    }

    @Test
    fun `handles double values correctly`() {
        val input = Double.MAX_VALUE
        val json = Moshi.asFormatString(input)
        assertThat(Moshi.parse(json), equalTo(MoshiDecimal(input)))
        assertThat(Moshi.asA<Double>(json), equalTo(input))
    }

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

    @Test
    fun `websocket message message json`() {
        assertThat(WsMessage().json(obj()), equalTo(WsMessage("""{}""")))
    }
}

class MoshiDataContainerTest {

    class MyValue private constructor(value: Boolean) : AbstractValue<Boolean>(value) {
        companion object : BooleanValueFactory<MyValue>(::MyValue)
    }

    class Foo(node: MoshiNode) : MoshiNodeDataContainer(node) {
        val foo by required<String>()
        var bar by required(MyValue)
    }

    @Test
    fun `can use custom data container to modify a MoshiNode`() {
        val json = """{"foo":"world","bar":true}"""

        val lens = Body.json(::Foo).toLens()

        val data = lens(Request(GET, "").body(json))
        assertThat(data.foo, equalTo("world"))
        assertThat(data.bar, equalTo(MyValue.of(true)))
        data.bar = MyValue.of(false)
        assertThat(data.bar, equalTo(MyValue.of(false)))

        val updated = Request(GET, "").with(lens of data)
        assertThat(updated, hasBody("""{"foo":"world","bar":false}"""))
    }
}
