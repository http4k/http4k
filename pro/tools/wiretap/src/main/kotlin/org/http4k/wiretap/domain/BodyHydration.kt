package org.http4k.wiretap.domain

import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response

enum class BodyHydration {
    None, RequestOnly, ResponseOnly, All;

    operator fun invoke(message: HttpMessage) = when (this) {
        All -> true
        RequestOnly -> message is Request
        ResponseOnly -> message is Response
        None -> false
    }
}
