package org.http4k.connect

import org.http4k.core.Request
import org.http4k.core.Response

interface Action<R> {
    fun toRequest(): Request
    fun toResult(response: Response): R
}
