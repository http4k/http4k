package org.http4k.hamkrest

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.anything
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.matches
import com.natpryce.hamkrest.present
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpMessage
import org.http4k.format.Json
import org.http4k.lens.BodyLens
import org.http4k.lens.Header
import org.http4k.lens.HeaderLens

internal fun <T : HttpMessage, R> httpMessageHas(name: String, feature: (T) -> R, featureMatcher: Matcher<R>): Matcher<T> = object : Matcher<T> {
    override fun invoke(actual: T) =
        featureMatcher(feature(actual)).let {
            when (it) {
                is MatchResult.Mismatch -> MatchResult.Mismatch("had $name that ${it.description}\nin: $actual")
                else -> it
            }
        }

    override val description = "has $name that ${featureMatcher.description}"
    override val negatedDescription = "does not have $name that ${featureMatcher.description}"
}

fun <T> hasHeader(lens: HeaderLens<T>, matcher: Matcher<T>): Matcher<HttpMessage> = LensMatcher(httpMessageHas("Header '${lens.meta.name}'", { req: HttpMessage -> lens(req) }, matcher))

@JvmName("hasBodyNullableString")
fun hasHeader(name: String, matcher: Matcher<String?>): Matcher<HttpMessage> = httpMessageHas("Header '$name'", { m: HttpMessage -> m.header(name) }, matcher)

fun hasHeader(name: String, matcher: Matcher<String>): Matcher<HttpMessage> = httpMessageHas("Header '$name'", { m: HttpMessage -> m.header(name) }, present(matcher))

fun hasHeader(name: String, expected: CharSequence): Matcher<HttpMessage> = hasHeader(name, equalTo(expected))

fun hasHeader(name: String, expected: Regex): Matcher<HttpMessage> = hasHeader(name, present(matches(expected)))

fun hasHeader(name: String): Matcher<HttpMessage> = httpMessageHas("Header '$name'", { m: HttpMessage -> m.header(name) }, present(anything))

fun hasHeader(name: String, expected: List<String?>): Matcher<HttpMessage> = httpMessageHas("Header '$name'", { m: HttpMessage -> m.headerValues(name) }, equalTo(expected))

fun hasContentType(expected: ContentType): Matcher<HttpMessage> = httpMessageHas("Content-Type", { m: HttpMessage -> Header.CONTENT_TYPE(m) }, equalTo(expected))

fun hasBody(expected: Matcher<Body>): Matcher<HttpMessage> = httpMessageHas("Body", { m: HttpMessage -> m.body }, expected)

@JvmName("hasBodyNullableString")
fun hasBody(expected: Matcher<String?>): Matcher<HttpMessage> = httpMessageHas("Body", { m: HttpMessage -> m.bodyString() }, expected)

@JvmName("hasBodyString")
fun hasBody(expected: Matcher<String>): Matcher<HttpMessage> = httpMessageHas("Body", { m: HttpMessage -> m.bodyString() }, expected)

fun hasBody(expected: CharSequence): Matcher<HttpMessage> = hasBody(equalTo<CharSequence>(expected))

fun hasBody(expected: Regex): Matcher<HttpMessage> = hasBody(present(matches(expected)))

fun <T> hasBody(lens: BodyLens<T>, matcher: Matcher<T>): Matcher<HttpMessage> = LensMatcher(httpMessageHas("Body", { m: HttpMessage -> lens(m) }, matcher))

fun <NODE : Any> Json<NODE>.hasBody(expected: NODE): Matcher<HttpMessage> = httpMessageHas("Body", { m: HttpMessage -> parse(m.bodyString()) }, equalTo(expected))

fun <NODE : Any> Json<NODE>.hasBody(expected: Matcher<NODE>): Matcher<HttpMessage> = httpMessageHas("Body", { m: HttpMessage -> parse(m.bodyString()) }, expected)

fun <NODE : Any> Json<NODE>.hasBody(expected: String): Matcher<HttpMessage> = httpMessageHas("Body", { m: HttpMessage -> compactify(m.bodyString()) }, equalTo(compactify(expected)))
