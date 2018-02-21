package org.http4k.format

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Moshi.auto
import org.junit.Test

class MoshiAutoTest : AutoMarshallingContract(Moshi) {

    override val expectedAutoMarshallingResult = """{"string":"hello","child":{"string":"world","numbers":[1],"bool":true},"numbers":[],"bool":false}"""

    @Test
    fun `roundtrip list of arbitary objects to and from body`() {
        val body = Body.auto<Array<ArbObject>>().toLens()

        assertThat(body(Response(Status.OK).with(body of arrayOf(obj))), equalTo(arrayOf(obj)))
    }

    @Test
    fun `roundtrip array of arbitary objects to and from JSON`() {
        val input = arrayOf(obj)
        val asJsonString = Moshi.asJsonString(input)
        assertThat(Moshi.asA(asJsonString), equalTo(input))
    }

    @Test
    fun `roundtrip list of arbitary objects to and from JSON`() {
        val jsonString = Moshi.asJsonString(listOf(obj), List::class)
        assertThat(Moshi.asA(jsonString), equalTo(arrayOf(obj)))
//        val fromJson = Moshi.adapterFor(List::class.java).fromJson(Moshi.adapterFor(List::class.java).toJson(listOf(obj)))
//        println(fromJson!![0])
    }
}
