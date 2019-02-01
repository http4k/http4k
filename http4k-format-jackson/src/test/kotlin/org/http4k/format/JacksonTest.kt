package org.http4k.format

import com.fasterxml.jackson.annotation.JsonView
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.http4k.format.Jackson.autoView
import org.http4k.hamkrest.hasBody
import org.http4k.jsonrpc.AutoMappingJsonRpcServiceContract
import org.http4k.jsonrpc.ManualMappingJsonRpcServiceContract
import org.http4k.lens.BiDiMapping
import org.http4k.lens.StringBiDiMappings
import org.http4k.websocket.WsMessage
import org.junit.jupiter.api.Test

open class Public
class Private : Public()

data class ArbObjectWithView(@JsonView(Private::class) @JvmField val priv: Int, @JsonView(Public::class) @JvmField val pub: Int)

class JacksonAutoTest : AutoMarshallingContract(Jackson) {

    @Test
    fun ` roundtrip arbitary object to and from JSON element`() {
        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)
        val out = Jackson.asJsonObject(obj)
        assertThat(Jackson.asA(out, ArbObject::class), equalTo(obj))
    }

    @Test
    fun `roundtrip list of arbitary objects to and from body`() {
        val body = Body.auto<Array<ArbObject>>().toLens()

        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(body(Response(Status.OK).with(body of arrayOf(obj))).toList(), equalTo(arrayOf(obj).toList()))
    }

    @Test
    fun `roundtrip body using view`() {
        val arbObjectWithView = ArbObjectWithView(3, 5)
        val publicLens = Body.autoView<ArbObjectWithView, Public>().toLens()
        val privateLens = Body.autoView<ArbObjectWithView, Private>().toLens()

        assertThat(Response(OK).with(publicLens of arbObjectWithView), hasBody(equalTo("""{"pub":5}""")))
        assertThat(Response(OK).with(privateLens of arbObjectWithView), hasBody(equalTo("""{"priv":3,"pub":5}""")))

        assertThat(publicLens(Response(OK).with(privateLens of arbObjectWithView)), equalTo(ArbObjectWithView(0, 5)))
    }

    @Test
    fun `roundtrip WsMessage using view`() {
        val arbObjectWithView = ArbObjectWithView(3, 5)
        val publicLens = WsMessage.autoView<ArbObjectWithView, Public>().toLens()
        val privateLens = WsMessage.autoView<ArbObjectWithView, Private>().toLens()

        assertThat(publicLens(arbObjectWithView).bodyString(), equalTo("""{"pub":5}"""))
        assertThat(privateLens(arbObjectWithView).bodyString(), equalTo("""{"priv":3,"pub":5}"""))

        assertThat(publicLens(privateLens(arbObjectWithView)), equalTo(ArbObjectWithView(0, 5)))
    }

    override fun customJson() = object : ConfigurableJackson(
        KotlinModule()
            .asConfigurable()
            .bigDecimal(BiDiMapping(::BigDecimalHolder, BigDecimalHolder::value))
            .bigInteger(BiDiMapping(::BigIntegerHolder, BigIntegerHolder::value))
            .boolean(BiDiMapping(::BooleanHolder, BooleanHolder::value))
            .text(StringBiDiMappings.bigDecimal().map(::MappedBigDecimalHolder, MappedBigDecimalHolder::value))
            .done()
    ) {}
}

class JacksonTest : JsonContract<JsonNode>(Jackson) {
    override val prettyString = """{
  "hello" : "world"
}"""
}

class JacksonJsonErrorResponseRendererTest : JsonErrorResponseRendererContract<JsonNode>(Jackson)
class JacksonGenerateDataClassesTest : GenerateDataClassesContract<JsonNode>(Jackson)
class JacksonAutoMappingJsonRpcServiceTest : AutoMappingJsonRpcServiceContract<JsonNode>(Jackson)
class JacksonManualMappingJsonRpcServiceTest : ManualMappingJsonRpcServiceContract<JsonNode>(Jackson)

