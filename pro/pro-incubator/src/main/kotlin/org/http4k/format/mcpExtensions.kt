package org.http4k.format

import org.http4k.jsonrpc.jsonRpcVersion

fun <NODE> Json<NODE>.renderNotification(method: String): NODE = this {
    obj(
        "jsonrpc" to string(jsonRpcVersion),
        "method" to string(method)
    )
}
