package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test

data class ArbObject(var string: String = "", var child: ArbObject? = null, var numbers: List<Int> = listOf(), var bool: Boolean = false)

abstract class AutoMarshallingContract(private val j: AutoMarshallingJson) {

    protected open val expectedAutoMarshallingResult = """{"string":"hello","child":{"string":"world","child":null,"numbers":[1],"bool":true},"numbers":[],"bool":false}"""

    val obj = ArbObject("hello", ArbObject("world", null, mutableListOf(1), true), emptyList(), false)

    @Test
    fun `roundtrip arbitrary object to and from JSON string`() {
        val out = j.asJsonString(obj)
        assertThat(out, equalTo(expectedAutoMarshallingResult))
        assertThat(j.asA(out, ArbObject::class), equalTo(obj))
    }

    @Test
    open fun `fails decoding when a required value is null`() {
        assertThat({j.asA("{}", ArbObject::class)}, throws<Exception>())
    }
}