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

    override val expectedAutoMarshallingResult = """{"bool":false,"child":{"bool":true,"numbers":[1],"string":"world"},"numbers":[],"string":"hello"}"""

    @Test
    fun `roundtrip list of arbitary objects to and from object`() {
        val body = Body.auto<Array<ArbObject>>().toLens()

        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(body(Response(Status.OK).with(body of arrayOf(obj))).asList(), equalTo(arrayOf(obj).asList()))
    }
}
