package org.http4k.kotest

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import org.http4k.lens.Lens
import org.http4k.lens.WebForm

fun <T> WebForm.shouldHaveFormField(field: Lens<WebForm, T>, matcher: Matcher<T>) = this should haveFormField(field, matcher)
fun <T> WebForm.shouldNotHaveFormField(field: Lens<WebForm, T>, matcher: Matcher<T>) = this shouldNot haveFormField(field, matcher)
fun <T> haveFormField(lens: Lens<WebForm, T>, matcher: Matcher<T>): Matcher<WebForm> = object : Matcher<WebForm> {
    override fun test(value: WebForm): MatcherResult {
        val result = LensMatcher(matcher.compose { form: WebForm -> lens(form) }).test(value)
        return MatcherResult(
            result.passed(),
            "Form should have field ${lens.meta.name} matching: ${result.failureMessage()}",
            "Form should not have field ${lens.meta.name} matching: ${result.negatedFailureMessage()}"
        )
    }
}
