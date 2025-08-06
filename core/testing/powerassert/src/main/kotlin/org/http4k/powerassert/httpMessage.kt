package org.http4k.powerassert

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpMessage
import org.http4k.format.Json
import org.http4k.lens.BodyLens
import org.http4k.lens.Header
import org.http4k.lens.HeaderLens
import org.http4k.lens.LensFailure

@Suppress("NOTHING_TO_INLINE")
inline fun <T> HttpMessage.hasHeader(lens: HeaderLens<T>, expected: T): Boolean = try {
    lens(this) == expected
} catch (e: LensFailure) {
    false
}

@Suppress("NOTHING_TO_INLINE")
inline fun HttpMessage.hasHeader(name: String, expected: String?): Boolean = header(name) == expected

@Suppress("NOTHING_TO_INLINE")
inline fun HttpMessage.hasHeader(name: String, expected: Regex): Boolean = header(name)?.let { expected.matches(it) } ?: false

@Suppress("NOTHING_TO_INLINE")
inline fun HttpMessage.hasHeader(name: String): Boolean = header(name) != null

@Suppress("NOTHING_TO_INLINE")
inline fun HttpMessage.hasHeader(name: String, expected: List<String?>): Boolean = headerValues(name) == expected

@Suppress("NOTHING_TO_INLINE")
inline fun HttpMessage.hasContentType(expected: ContentType): Boolean = try {
    Header.CONTENT_TYPE(this) == expected
} catch (e: LensFailure) {
    false
}

@Suppress("NOTHING_TO_INLINE")
inline fun HttpMessage.hasBody(expected: Body): Boolean = body == expected

@Suppress("NOTHING_TO_INLINE")
inline fun HttpMessage.hasBody(expected: String?): Boolean = bodyString() == expected

@Suppress("NOTHING_TO_INLINE")
inline fun HttpMessage.hasBody(expected: Regex): Boolean = expected.matches(bodyString())

@Suppress("NOTHING_TO_INLINE")
inline fun <T> HttpMessage.hasBody(lens: BodyLens<T>, expected: T): Boolean = try {
    lens(this) == expected
} catch (e: LensFailure) {
    false
}

@Suppress("NOTHING_TO_INLINE")
inline fun <NODE> Json<NODE>.hasBody(message: HttpMessage, expected: NODE): Boolean = try {
    parse(message.bodyString()) == expected
} catch (e: Exception) {
    false
}

@Suppress("NOTHING_TO_INLINE")
inline fun <NODE> Json<NODE>.hasBody(message: HttpMessage, expected: String): Boolean = try {
    compactify(message.bodyString()) == compactify(expected)
} catch (e: Exception) {
    false
}
