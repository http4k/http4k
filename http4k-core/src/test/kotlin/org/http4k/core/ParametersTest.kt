package org.http4k.core

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class ParametersTest {
    @Test
    fun extract_query_parameters() {
        assertThat("foo=one&bar=two".toParameters().findSingle("foo")!!, equalTo("one"))
        assertThat("foo=&bar=two".toParameters().findSingle("foo")!!, equalTo(""))
        assertThat("foo&bar=two".toParameters().findSingle("foo"), absent())
        assertThat("foo&bar=two".toParameters().findSingle("notthere"), absent())
    }

    @Test
    fun query_parameters_are_decoded() {
        assertThat("foo=one+two&bar=three".toParameters().findSingle("foo")!!, equalTo("one two"))
        assertThat("foo=one&super%2Fbar=two".toParameters().findSingle("super/bar")!!, equalTo("two"))
    }

    @Test
    fun round_trip() {
        assertThat("a=1&b&c+d=three+four&a=2".toParameters().toUrlFormEncoded(), equalTo("a=1&b&c+d=three+four&a=2"))
    }

    @Test
    fun `can convert parameters to map`() {
        val map: Map<String, List<String?>> = listOf("a" to "A", "b" to "B", "a" to null).toParametersMap()

        assertThat(map["a"], equalTo(listOf("A", null)))
        assertThat(map["b"], equalTo<List<String?>>(listOf("B")))
        assertThat(map["c"], absent())

        assertThat(map.getFirst("a"), equalTo("A"))
        assertThat(map.getFirst("b"), equalTo("B"))
        assertThat(map.getFirst("c"), absent())
    }
}