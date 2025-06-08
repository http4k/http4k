package chatzilla.endpoints.elicitations

import chatzilla.ChatHistory
import org.http4k.ai.llm.util.LLMJson
import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.ElicitationAction.accept
import org.http4k.ai.mcp.model.Meta
import org.http4k.core.Method.GET
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.sendMergeFragments
import org.http4k.template.DatastarFragmentRenderer

fun GetElicitations(history: ChatHistory, renderer: DatastarFragmentRenderer, client: McpClient) =
    "/elicitations" bind sse(GET to sse { sse ->
        client.elicitations().onElicitation {
            System.err.println(it)
            sse.sendMergeFragments(renderer(ElicitationForm(it)))
            Thread.sleep(100000)
            ElicitationResponse(accept, LLMJson.obj(), Meta(it.progressToken))
        }
    })

