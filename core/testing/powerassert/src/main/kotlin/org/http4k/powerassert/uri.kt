package org.http4k.powerassert

import org.http4k.core.Uri

@Suppress("NOTHING_TO_INLINE")
inline fun Uri.hasPath(expected: String?): Boolean = path == expected

@Suppress("NOTHING_TO_INLINE")
inline fun Uri.hasPath(expected: Regex): Boolean = path.let { expected.matches(it) }

@Suppress("NOTHING_TO_INLINE")
inline fun Uri.hasQuery(expected: String): Boolean = query == expected

@Suppress("NOTHING_TO_INLINE")
inline fun Uri.hasAuthority(expected: String): Boolean = authority == expected

@Suppress("NOTHING_TO_INLINE")
inline fun Uri.hasHost(expected: String): Boolean = host == expected

@Suppress("NOTHING_TO_INLINE")
inline fun Uri.hasPort(expected: Int): Boolean = port == expected
