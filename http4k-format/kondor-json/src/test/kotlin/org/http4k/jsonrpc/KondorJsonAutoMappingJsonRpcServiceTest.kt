package org.http4k.jsonrpc

import com.ubertob.kondor.json.JAny
import com.ubertob.kondor.json.JInt
import com.ubertob.kondor.json.jsonnode.JsonNode
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.num
import org.http4k.format.KondorJson
import org.http4k.format.register

class KondorJsonAutoMappingJsonRpcServiceTest : AutoMappingJsonRpcServiceContract<JsonNode>(
    KondorJson()
        .register(JCounterIncrement)
        .register(JInt)
)

private object JCounterIncrement : JAny<Counter.Increment>() {
    val value by num(Counter.Increment::value)

    override fun JsonNodeObject.deserializeOrThrow() = Counter.Increment(value = +value)
}
