package org.http4k.kotest

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.be
import io.kotest.matchers.neverNullMatcher
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.SameSite
import java.time.LocalDateTime

infix fun Cookie.shouldHaveName(expected: String) = this should haveName(expected)
infix fun Cookie.shouldNotHaveName(expected: String) = this shouldNot haveName(expected)
fun haveName(expected: String): Matcher<Cookie> = object : Matcher<Cookie> {
    override fun test(value: Cookie): MatcherResult = MatcherResult(
        value.name == expected,
        "Cookie should have name $expected but was ${value.name}",
        "Cookie should not have name $expected")
}

infix fun Cookie.shouldHaveValue(expected: String) = this should haveValue(expected)
infix fun Cookie.shouldNotHaveValue(expected: String) = this shouldNot haveValue(expected)
fun haveValue(expected: String): Matcher<Cookie> = haveValue(be<String>(expected))

@JvmName("haveCookieValueNullableString")
fun haveValue(matcher: Matcher<String?>): Matcher<Cookie> = object : Matcher<Cookie> {
    override fun test(value: Cookie): MatcherResult {
        val testResult = matcher.test(value.value)
        return MatcherResult(
            testResult.passed(),
            "Cookie value mismatch: ${testResult.failureMessage()}",
            "Cookie value mismatch: ${testResult.negatedFailureMessage()}")
    }
}

fun haveValue(matcher: Matcher<String>): Matcher<Cookie> = haveValue(neverNullMatcher(matcher::test))

infix fun Cookie.shouldHaveDomain(expected: String) = this should haveDomain(expected)
infix fun Cookie.shouldNotHaveDomain(expected: String) = this shouldNot haveDomain(expected)
fun haveDomain(expected: String): Matcher<Cookie> = object : Matcher<Cookie> {
    override fun test(value: Cookie): MatcherResult = MatcherResult(
        value.domain == expected,
        "Cookie domain should be $expected but was ${value.domain}",
        "Cookie domain should not be $expected")
}

infix fun Cookie.shouldHavePath(expected: String) = this should haveCookiePath(expected)
infix fun Cookie.shouldNotHavePath(expected: String) = this shouldNot haveCookiePath(expected)
fun haveCookiePath(expected: String): Matcher<Cookie> = object : Matcher<Cookie> {
    override fun test(value: Cookie): MatcherResult = MatcherResult(
        value.path == expected,
        "Cookie path should be $expected but was ${value.path}",
        "Cookie path should not be $expected")
}

fun Cookie.shouldBeSecure() = this should beSecure()
fun Cookie.shouldNotBeSecure() = this shouldNot beSecure()
fun beSecure(): Matcher<Cookie> = object : Matcher<Cookie> {
    override fun test(value: Cookie): MatcherResult = MatcherResult(
        value.secure,
        "Cookie should be secure",
        "Cookie should not be secure")
}

fun Cookie.shouldBeHttpOnly() = this should beHttpOnly()
fun Cookie.shouldNotBeHttpOnly() = this shouldNot beHttpOnly()
fun beHttpOnly(): Matcher<Cookie> = object : Matcher<Cookie> {
    override fun test(value: Cookie): MatcherResult = MatcherResult(
        value.httpOnly,
        "Cookie should be httpOnly",
        "Cookie should not be httpOnly")
}

infix fun Cookie.shouldHaveExpiry(expected: LocalDateTime) = this should expireOn(expected)
infix fun Cookie.shouldNotHaveExpiry(expected: LocalDateTime) = this shouldNot expireOn(expected)
fun expireOn(expected: LocalDateTime): Matcher<Cookie> = expireOn(be<LocalDateTime?>(expected))
fun expireOn(matcher: Matcher<LocalDateTime?>): Matcher<Cookie> = object : Matcher<Cookie> {
    override fun test(value: Cookie): MatcherResult {
        val testResult = matcher.test(value.expires)
        return MatcherResult(
            testResult.passed(),
            "Cookie expiration mismatch: ${testResult.failureMessage()}",
            "Cookie expiration mismatch: ${testResult.negatedFailureMessage()}")
    }
}

fun Cookie.shouldNeverExpire() = this should neverExpire()
fun neverExpire(): Matcher<Cookie> = object : Matcher<Cookie> {
    override fun test(value: Cookie): MatcherResult = MatcherResult(
        value.expires == null,
        "Cookie should never expire (expires on ${value.expires})",
        "Cookie should expire")
}

infix fun Cookie.shouldHaveSameSite(expected: SameSite) = this should haveSameSite(expected)
infix fun Cookie.shouldNotHaveSameSite(expected: SameSite) = this shouldNot haveSameSite(expected)
fun haveSameSite(expected: SameSite): Matcher<Cookie> = object : Matcher<Cookie> {
    override fun test(value: Cookie): MatcherResult = MatcherResult(
        value.sameSite == expected,
        "Cookie should have same-site: expected:<$expected> but was:<${value.sameSite}>",
        "Cookie should not have same-site $expected")
}
