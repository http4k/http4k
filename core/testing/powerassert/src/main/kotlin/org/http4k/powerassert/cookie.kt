package org.http4k.powerassert

import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.SameSite
import java.time.Instant

@Suppress("NOTHING_TO_INLINE")
inline fun Cookie.hasName(expected: CharSequence): Boolean = name == expected.toString()

@Suppress("NOTHING_TO_INLINE")
inline fun Cookie.hasValue(expected: String?): Boolean = value == expected

@Suppress("NOTHING_TO_INLINE")
inline fun Cookie.hasDomain(expected: CharSequence): Boolean = domain == expected.toString()

@Suppress("NOTHING_TO_INLINE")
inline fun Cookie.hasPath(expected: CharSequence): Boolean = path == expected.toString()

@Suppress("NOTHING_TO_INLINE")
inline fun Cookie.isSecure(expected: Boolean = true): Boolean = secure == expected

@Suppress("NOTHING_TO_INLINE")
inline fun Cookie.isHttpOnly(expected: Boolean = true): Boolean = httpOnly == expected

@Suppress("NOTHING_TO_INLINE")
inline fun Cookie.hasExpiry(expected: Instant): Boolean = expires == expected

@Suppress("NOTHING_TO_INLINE")
inline fun Cookie.hasSameSite(expected: SameSite): Boolean = sameSite == expected