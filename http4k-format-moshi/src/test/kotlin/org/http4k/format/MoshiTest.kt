package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Moshi.auto
import org.http4k.lens.BiDiMapping
import org.junit.jupiter.api.Test

class MoshiAutoTest : AutoMarshallingContract(Moshi) {

    override val expectedAutoMarshallingResult = """{"string":"hello","child":{"string":"world","numbers":[1],"bool":true},"numbers":[],"bool":false}"""

    @Test
    fun `roundtrip list of arbitary objects to and from body`() {
        val body = Body.auto<Array<ArbObject>>().toLens()

        val expected = arrayOf(obj)
        val actual = body(Response(Status.OK).with(body of expected))
        assertThat(actual.toList(), equalTo(expected.toList()))
    }

    @Test
    fun `roundtrip array of arbitary objects to and from JSON`() {
        val expected = arrayOf(obj)
        val asJsonString = Moshi.asJsonString(expected)
        val actual: Array<ArbObject> = Moshi.asA(asJsonString)
        assertThat(actual.toList(), equalTo(expected.toList()))
    }

    @Test
    fun `roundtrip list of arbitary objects to and from JSON`() {
        val jsonString = Moshi.asJsonString(listOf(obj), List::class)
        val actual = Moshi.asA<Array<ArbObject>>(jsonString)
        val expected = arrayOf(obj)
        assertThat(actual.toList().toString(), actual.toList(), equalTo(expected.toList()))
    }

    override fun customJson() = object : ConfigurableMoshi(
        com.squareup.moshi.Moshi.Builder()
            .asConfigurable()
            .decimal(BiDiMapping(::BigDecimalHolder, BigDecimalHolder::value))
            .number(BiDiMapping(::BigIntegerHolder, BigIntegerHolder::value))
            .boolean(BiDiMapping(::BooleanHolder, BooleanHolder::value))
            .done()
    ) {}
}
