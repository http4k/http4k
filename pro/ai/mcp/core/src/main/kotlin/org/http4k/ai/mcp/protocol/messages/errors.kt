package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.util.McpJson
import org.http4k.format.Json
import org.http4k.jsonrpc.ErrorMessage
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class URLElicitationRequiredError(
    val elicitations: List<McpElicitations.Request.Url>,
    override val message: String
) :
    ErrorMessage(CODE, message) {

    override fun <NODE> data(json: Json<NODE>): NODE = json {
        obj("elicitations" to array(elicitations.map { json.parse(McpJson.asFormatString(it)) }))
    }

    companion object {
        val CODE = -32042
    }
}
