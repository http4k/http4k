package org.http4k.powerassert

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.lens.LensFailure
import org.http4k.lens.QueryLens

@Suppress("NOTHING_TO_INLINE")
inline fun <T> Request.hasQuery(lens: QueryLens<T>, expected: T): Boolean = try {
    lens(this) == expected
} catch (e: LensFailure) {
    false
}

@Suppress("NOTHING_TO_INLINE")
inline fun Request.hasQuery(name: String, expected: String?): Boolean = query(name) == expected

@Suppress("NOTHING_TO_INLINE")
inline fun Request.hasQuery(name: String, expected: Regex): Boolean = query(name)?.let { expected.matches(it) } ?: false

@Suppress("NOTHING_TO_INLINE")
inline fun Request.hasQuery(name: String, expected: List<String?>): Boolean = queries(name) == expected

@Suppress("NOTHING_TO_INLINE")
inline fun Request.hasForm(name: String, expected: String?): Boolean = form(name) == expected

@Suppress("NOTHING_TO_INLINE")
inline fun Request.hasForm(name: String, expected: Regex): Boolean = form(name)?.let { expected.matches(it) } ?: false

@Suppress("NOTHING_TO_INLINE")
inline fun Request.hasMethod(expected: Method): Boolean = method == expected

@Suppress("NOTHING_TO_INLINE")
inline fun Request.hasUri(expected: Regex): Boolean = expected.matches(uri.toString())

@Suppress("NOTHING_TO_INLINE")
inline fun Request.hasUri(expected: Uri): Boolean = uri == expected

@Suppress("NOTHING_TO_INLINE")
inline fun Request.hasUri(expected: String): Boolean = uri.toString() == expected

@Suppress("NOTHING_TO_INLINE")
inline fun Request.hasCookie(expected: Cookie): Boolean = cookie(expected.name) == expected

@Suppress("NOTHING_TO_INLINE")
inline fun Request.hasCookie(name: String, expected: Regex): Boolean = 
    cookie(name)?.value?.let { expected.matches(it) } ?: false

@Suppress("NOTHING_TO_INLINE")
inline fun Request.hasCookie(name: String, expected: String): Boolean = 
    cookie(name)?.value == expected

@Suppress("NOTHING_TO_INLINE")
inline fun Request.hasCookie(name: String, expected: Cookie): Boolean = cookie(name) == expected
