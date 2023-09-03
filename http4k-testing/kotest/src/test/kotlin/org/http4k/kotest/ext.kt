package org.http4k.kotest

import io.kotest.matchers.Matcher
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import org.junit.jupiter.api.assertThrows

internal fun <T> assertMatchAndNonMatch(t: T, match: Matcher<T>, mismatch: Matcher<T>) {
    t should match
    assertThrows<AssertionError>  { t shouldNot match }

    t shouldNot mismatch

    assertThrows<AssertionError>  { t should mismatch }
}

internal fun <T> assertMatchAndNonMatch(t: T, match: T.() -> Unit, mismatch: T.() -> Unit) {
    match(t)
    assertThrows<AssertionError>  { mismatch(t) }
}
