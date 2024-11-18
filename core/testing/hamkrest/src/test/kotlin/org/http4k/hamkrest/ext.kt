package org.http4k.hamkrest

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.throws

fun <T> assertMatchAndNonMatch(t: T, match: Matcher<T>, mismatch: Matcher<T>) {
    assertThat(t, match)
    assertThat({ assertThat(t, mismatch) }, throws<AssertionError>())
}
