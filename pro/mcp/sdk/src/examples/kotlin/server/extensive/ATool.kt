package server.extensive

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Tool
import org.http4k.routing.bind

fun aTool(hub: Hub) = Tool("count", "do a count") bind {
    hub.doSomethingWithService()
        .map { ToolResponse.Ok(listOf(Content.Text("the number is $it"))) }
        .mapFailure { ToolResponse.Error(InternalError) }
        .get()
}
