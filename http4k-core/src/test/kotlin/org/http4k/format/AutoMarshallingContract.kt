package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

data class ArbObject(val string: String, val child: ArbObject?, val numbers: List<Int>, val bool: Boolean)

abstract class AutoMarshallingContract(private val j: AutoMarshallingJson) {

    protected open val expectedAutoMarshallingResult = """{"string":"hello","child":{"string":"world","child":null,"numbers":[1],"bool":true},"numbers":[],"bool":false}"""
    @Test
    fun `roundtrip arbitary object to and from JSON string`() {
        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)
        val out = j.asJsonString(obj)
        assertThat(out, equalTo(expectedAutoMarshallingResult))
        assertThat(j.asA(out, ArbObject::class), equalTo(obj))
    }
}