package org.http4k.powerassert

import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookies

@Suppress("NOTHING_TO_INLINE")
inline fun Response.hasStatus(expected: Status): Boolean = status == expected

@Suppress("NOTHING_TO_INLINE")
inline fun Response.hasStatusDescription(expected: String): Boolean = status.description == expected

@Suppress("NOTHING_TO_INLINE")
inline fun Response.hasSetCookie(expected: Cookie): Boolean = cookies().any { it == expected }

@Suppress("NOTHING_TO_INLINE")
inline fun Response.hasSetCookie(name: String, expected: Cookie): Boolean = 
    cookies().find { it.name == name } == expected

@Suppress("NOTHING_TO_INLINE")
inline fun Response.hasSetCookie(name: String): Boolean = 
    cookies().any { it.name == name }