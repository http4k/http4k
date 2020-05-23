package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.squareup.moshi.Moshi.Builder
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Moshi.auto
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class MoshiAutoTest : AutoMarshallingContract(Moshi) {

    override val expectedAutoMarshallingResult = """{"string":"hello","child":{"string":"world","numbers":[1],"bool":true},"numbers":[],"bool":false}"""

    @Test
    @Disabled("Currently doesn't work because of need for custom list adapters")
    fun `roundtrip list of arbitary objects to and from body`() {
        val body = Body.auto<List<ArbObject>>().toLens()

        val expected = listOf(obj)
        val actual = body(Response(OK).with(body of expected))
        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `roundtrip array of arbitary objects to and from JSON`() {
        val expected = arrayOf(obj)
        val asJsonString = Moshi.asString(expected)
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

    override fun customJson() = object : ConfigurableMoshi(Builder().asConfigurable().customise()) {}
}
