package org.http4k.kotest

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.be
import io.kotest.matchers.neverNullMatcher
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import io.kotest.matchers.string.contain
import org.http4k.core.Uri

internal fun <R> uriHas(name: String, extractValue: (Uri) -> R, match: Matcher<R>): Matcher<Uri> = object : Matcher<Uri> {
    override fun test(value: Uri): MatcherResult {
        val testResult = match.test(extractValue(value))
        return MatcherResult(
            testResult.passed(),
            "Invalid Uri $name: ${testResult.failureMessage()}",
            "Invalid Uri $name: ${testResult.negatedFailureMessage()}")
    }
}

infix fun Uri.shouldHavePath(match: Matcher<String?>) = this should havePath(match)
infix fun Uri.shouldNotHavePath(match: Matcher<String?>) = this shouldNot havePath(match)
fun havePath(matcher: Matcher<String?>): Matcher<Uri> = uriHas("path", Uri::path, matcher)

infix fun Uri.shouldHavePath(expected: String?) = this should havePath(expected)
infix fun Uri.shouldNotHavePath(expected: String?) = this shouldNot havePath(expected)
fun havePath(expected: String?): Matcher<Uri> = havePath(be(expected))

infix fun Uri.shouldHavePath(expected: Regex) = this should havePath(expected)
infix fun Uri.shouldNotHavePath(expected: Regex) = this shouldNot havePath(expected)
fun havePath(expected: Regex): Matcher<Uri> = havePath(contain(expected))

infix fun Uri.shouldHaveQuery(expected: String) = this should haveQuery(expected)
infix fun Uri.shouldNotHaveQuery(expected: String) = this shouldNot haveQuery(expected)
fun haveQuery(expected: String): Matcher<Uri> = uriHas("query", Uri::query, be(expected))

infix fun Uri.shouldHaveAuthority(expected: String) = this should haveAuthority(expected)
infix fun Uri.shouldNotHaveAuthority(expected: String) = this shouldNot haveAuthority(expected)
fun haveAuthority(expected: String): Matcher<Uri> = uriHas("authority", Uri::authority, be(expected))

infix fun Uri.shouldHaveHost(expected: String) = this should haveHost(expected)
infix fun Uri.shouldNotHaveHost(expected: String) = this shouldNot haveHost(expected)
fun haveHost(expected: String): Matcher<Uri> = uriHas("host", Uri::host, be(expected))

infix fun Uri.shouldHavePort(expected: Int) = this should havePort(expected)
infix fun Uri.shouldNotHavePort(expected: Int) = this shouldNot havePort(expected)
fun havePort(expected: Int): Matcher<Uri> = uriHas("port", Uri::port, neverNullMatcher(be(expected)::test))
