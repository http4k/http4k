package org.http4k.hamkrest

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import org.http4k.lens.LensFailure

internal class LensMatcher<in R>(private val matcher: Matcher<R>) : Matcher<R> {
    override val description = matcher.description

    override fun invoke(actual: R): MatchResult = try {
        matcher(actual)
    } catch (e: LensFailure) {
        MatchResult.Mismatch("lens could not extract valid value from: $actual")
    }
}
