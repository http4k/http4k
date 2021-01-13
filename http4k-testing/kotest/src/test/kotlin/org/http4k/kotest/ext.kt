package org.http4k.kotest

import io.kotest.assertions.shouldFail
import io.kotest.matchers.Matcher
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot

internal fun <T> assertMatchAndNonMatch(t: T, match: Matcher<T>, mismatch: Matcher<T>) {
    t should match
    shouldFail { t shouldNot match }

    t shouldNot mismatch
    shouldFail { t should mismatch }
}
