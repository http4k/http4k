package org.http4k.jsonrpc

data class JsonRpcMethodBinding<IN, OUT>(val name: String, val handler: JsonRpcHandler<IN, OUT>)
