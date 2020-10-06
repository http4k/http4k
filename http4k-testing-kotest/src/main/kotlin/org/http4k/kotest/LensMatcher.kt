package org.http4k.kotest

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import org.http4k.lens.LensFailure

fun <R> LensMatcher(matcher: Matcher<R>) = object : Matcher<R> {
    override fun test(value: R): MatcherResult =
        try {
            matcher.test(value)
        } catch (e: LensFailure) {
            MatcherResult(
                passed = false,
                failureMessageFn = { "lens could not extract valid value from: $value" },
                negatedFailureMessageFn = { "lens could not extract valid value from: $value" }
            )
        }
}
