package org.http4k.kotest

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.and
import io.kotest.matchers.be
import io.kotest.matchers.neverNullMatcher
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import io.kotest.matchers.string.contain
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpMessage
import org.http4k.format.Json
import org.http4k.lens.BodyLens
import org.http4k.lens.Header
import org.http4k.lens.HeaderLens

fun <T> HttpMessage.shouldHaveHeader(lens: HeaderLens<T>, matcher: Matcher<T>) = this should haveHeader(lens, matcher)
fun <T> HttpMessage.shouldNotHaveHeader(lens: HeaderLens<T>, matcher: Matcher<T>) = this shouldNot haveHeader(lens, matcher)
fun <T> haveHeader(lens: HeaderLens<T>, matcher: Matcher<T>): Matcher<HttpMessage> = LensMatcher(httpMessageHas("Header \"${lens.meta.name}\"", { req: HttpMessage -> lens(req) }, matcher))

@JvmName("haveBodyNullableString")
fun haveHeader(name: String, matcher: Matcher<String?>): Matcher<HttpMessage> = httpMessageHas("Header \"$name\"", { m: HttpMessage -> m.header(name) }, matcher)

fun HttpMessage.shouldHaveHeader(name: String, matcher: Matcher<String>) = this should haveHeader(name, matcher)
fun HttpMessage.shouldNotHaveHeader(name: String, matcher: Matcher<String>) = this shouldNot haveHeader(name, matcher)
fun haveHeader(name: String, matcher: Matcher<String>): Matcher<HttpMessage> = httpMessageHas("Header \"$name\"", { m: HttpMessage -> m.header(name) }, neverNullMatcher { matcher.test(it) })

fun HttpMessage.shouldHaveHeader(name: String, expected: String) = this should haveHeader(name, expected)
fun HttpMessage.shouldNotHaveHeader(name: String, expected: String) = this shouldNot haveHeader(name, expected)
fun haveHeader(name: String, expected: String): Matcher<HttpMessage> = haveHeader(name, be(expected.toString()))

fun HttpMessage.shouldHaveHeader(name: String, expected: Regex) = this should haveHeader(name, expected)
fun HttpMessage.shouldNotHaveHeader(name: String, expected: Regex) = this shouldNot haveHeader(name, expected)
fun haveHeader(name: String, expected: Regex): Matcher<HttpMessage> = haveHeader(name, contain(expected))

infix fun HttpMessage.shouldHaveHeader(name: String) = this should haveHeader(name)
infix fun HttpMessage.shouldNotHaveHeader(name: String) = this shouldNot haveHeader(name)
fun haveHeader(name: String): Matcher<HttpMessage> = httpMessageHas("Header \"$name\"", { m: HttpMessage -> m.header(name) }, beNull().invert())

fun HttpMessage.shouldHaveHeader(name: String, expected: List<String?>) = this should haveHeader(name, expected)
fun HttpMessage.shouldNotHaveHeader(name: String, expected: List<String?>) = this shouldNot haveHeader(name, expected)
fun haveHeader(name: String, expected: List<String?>): Matcher<HttpMessage> = httpMessageHas("Header \"$name\"", { m: HttpMessage -> m.headerValues(name) }, be(expected))

infix fun HttpMessage.shouldHaveContentType(expected: ContentType) = this should haveContentType(expected)
infix fun HttpMessage.shouldNotHaveContentType(expected: ContentType) = this shouldNot haveContentType(expected)
fun haveContentType(expected: ContentType): Matcher<HttpMessage> = httpMessageHas("Content-Type", { m: HttpMessage -> Header.CONTENT_TYPE(m) }, beNull().invert().and(be(expected)))

infix fun HttpMessage.shouldHaveBody(expected: Matcher<Body>) = this should haveBody(expected)
infix fun HttpMessage.shouldNotHaveBody(expected: Matcher<Body>) = this shouldNot haveBody(expected)
fun <T : HttpMessage> haveBody(expected: Matcher<Body>): Matcher<T> = httpMessageHas("Body", { m: HttpMessage -> m.body }, expected)

@JvmName("shouldHaveBodyNullableStringMatcher")
infix fun HttpMessage.shouldHaveBody(expected: Matcher<String?>) = this should haveBody(expected)
@JvmName("shouldNotHaveBodyNullableStringMatcher")
infix fun HttpMessage.shouldNotHaveBody(expected: Matcher<String?>) = this shouldNot haveBody(expected)
@JvmName("haveBodyNullableStringMatcher")
fun <T : HttpMessage> haveBody(expected: Matcher<String?>): Matcher<T> = httpMessageHas("Body", { m: HttpMessage -> m.bodyString() }, expected)

@JvmName("shouldHaveBodyStringMatcher")
infix fun HttpMessage.shouldHaveBody(expected: Matcher<String>) = this should haveBody(expected)
@JvmName("shouldNotHaveBodyStringMatcher")
infix fun HttpMessage.shouldNotHaveBody(expected: Matcher<String>) = this shouldNot haveBody(expected)
@JvmName("haveBodyStringMatcher")
fun <T : HttpMessage> haveBody(expected: Matcher<String>): Matcher<T> = httpMessageHas("Body", { m: HttpMessage -> m.bodyString() }, expected)

infix fun HttpMessage.shouldHaveBody(expected: String) = this should haveBody(expected)
infix fun HttpMessage.shouldNotHaveBody(expected: String) = this shouldNot haveBody(expected)
fun <T : HttpMessage> haveBody(expected: String): Matcher<T> {
    val be: Matcher<String> = be(expected)
    return haveBody(be)
}

infix fun HttpMessage.shouldHaveBody(expected: Regex) = this should haveBody(expected)
infix fun HttpMessage.shouldNotHaveBody(expected: Regex) = this shouldNot haveBody(expected)
fun <T : HttpMessage> haveBody(expected: Regex): Matcher<T> = haveBody(contain(expected))

fun <T> HttpMessage.shouldHaveBody(lens: BodyLens<T>, matcher: Matcher<T>) = this should haveBody(lens, matcher)
fun <T> HttpMessage.shouldNotHaveBody(lens: BodyLens<T>, matcher: Matcher<T>) = this shouldNot haveBody(lens, matcher)
fun <T : HttpMessage, B> haveBody(lens: BodyLens<B>, matcher: Matcher<B>): Matcher<T> = LensMatcher(httpMessageHas("Body", { m: T -> lens(m) }, matcher))

fun <NODE> Json<NODE>.haveBody(expected: NODE): Matcher<HttpMessage> = httpMessageHas("Body", { m: HttpMessage -> compact(parse(m.bodyString())) }, be(compact(expected)))

// Removed because of : https://github.com/kotest/kotest/issues/1727
//fun <NODE> Json<NODE>.haveBody(expected: Matcher<NODE>): Matcher<HttpMessage> = httpMessageHas("Body", { m: HttpMessage -> parse(m.bodyString()) }, expected)

fun <NODE> Json<NODE>.haveBody(expected: String): Matcher<HttpMessage> = httpMessageHas("Body", { m: HttpMessage -> compactify(m.bodyString()) }, be(compactify(expected)))

fun <T : HttpMessage, R> httpMessageHas(name: String, extractValue: (T) -> R, match: Matcher<R>): Matcher<T> = object : Matcher<T> {
    override fun test(value: T): MatcherResult {
        val testResult = match.test(extractValue(value))
        return MatcherResult(
            testResult.passed(),
            "$name: ${testResult.failureMessage()}",
            "$name: ${testResult.negatedFailureMessage()}")
    }
}
