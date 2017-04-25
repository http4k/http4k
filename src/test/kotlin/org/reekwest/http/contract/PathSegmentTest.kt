package org.reekwest.http.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test

class PathSegmentTest {

    @Test
    fun `value present`() {
        assertThat(PathSegment.of("hello")("world"), equalTo("world"))
        assertThat(PathSegment.map { it.length }.of("hello")("world"), equalTo(5))
    }

    @Test
    fun `invalid value`() {
        val path = PathSegment.map(String::toInt).of("hello")
        assertThat({ path("world") }, throws(equalTo(ContractBreach(Invalid(path)))))
    }

    @Test
    fun `can create a custom type and get it`() {
        val path = PathSegment.map(::MyCustomBodyType).of("bob")
        assertThat(path("hello world!"), equalTo(MyCustomBodyType("hello world!")))
    }

    @Test
    fun `toString is ok`() {
        assertThat(PathSegment.of("hello").toString(), equalTo("Required path 'hello'"))
    }
}