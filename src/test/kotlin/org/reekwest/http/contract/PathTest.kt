package org.reekwest.http.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test

class PathTest {

    @Test
    fun `value present`() {
        assertThat(Path.of("hello")("world"), equalTo("world"))
        assertThat(Path.map { it.length }.of("hello")("world"), equalTo(5))
    }

    @Test
    fun `invalid value`() {
        val path = Path.map(String::toInt).of("hello")
        assertThat({ path("world") }, throws(equalTo(ContractBreach(Invalid(path)))))
    }

    @Test
    fun `can create a custom type and get it`() {
        val path = Path.map(::MyCustomBodyType).of("bob")
        assertThat(path("hello world!"), equalTo(MyCustomBodyType("hello world!")))
    }

    @Test
    fun `toString is ok`() {
        assertThat(Path.of("hello").toString(), equalTo("Required path 'hello'"))
    }
}