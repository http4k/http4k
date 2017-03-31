package com.gourame.http.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import junit.framework.TestCase
import org.junit.Test

class ParametersTest {
    @Test
    fun extract_query_parameters() {
        assertThat("foo=one&bar=two".toParameters().findSingle("foo")!!, equalTo("one"))
        assertThat("foo=&bar=two".toParameters().findSingle("foo")!!, equalTo(""))
        TestCase.assertNull("foo&bar=two".toParameters().findSingle("foo"))
        TestCase.assertNull("foo&bar=two".toParameters().findSingle("notthere"))
    }

    @Test
    fun query_parameters_are_decoded() {
        assertThat("foo=one+two&bar=three".toParameters().findSingle("foo")!!, equalTo("one two"))
        assertThat("foo=one&super%2Fbar=two".toParameters().findSingle("super/bar")!!, equalTo("two"))
    }
}