package server.extensive

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.orThrow
import org.http4k.core.Uri
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.model.Reference
import org.http4k.mcp.server.capability.ServerCapability
import org.http4k.routing.bind

fun aCompletion(hub: Hub): ServerCapability =
    Reference.of(Uri.of("foobar")) bind {
        hub.doSomethingToRepo().map { CompletionResponse(it) }.orThrow()
    }
