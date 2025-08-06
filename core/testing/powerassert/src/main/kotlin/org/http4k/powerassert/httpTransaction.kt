package org.http4k.powerassert

import org.http4k.core.HttpTransaction
import org.http4k.core.Request
import org.http4k.core.Response

@Suppress("NOTHING_TO_INLINE")
inline fun HttpTransaction.hasRequest(expected: Request): Boolean = request == expected

@Suppress("NOTHING_TO_INLINE")
inline fun HttpTransaction.hasResponse(expected: Response): Boolean = response == expected