package org.http4k.jsonrpc

sealed class JsonRpcMessage<NODE>(
    protected val fields: Map<String, NODE>
) {
    abstract val id: NODE?
}
