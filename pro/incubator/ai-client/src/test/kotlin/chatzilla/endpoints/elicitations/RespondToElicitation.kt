package chatzilla.endpoints.elicitations

import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.model.ElicitationAction
import org.http4k.ai.mcp.model.Meta
import org.http4k.lens.Path
import org.http4k.lens.enum
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.security.oauth.format.OAuthMoshi.datastarModel
import org.http4k.sse.sendMergeFragments
import org.http4k.template.DatastarFragmentRenderer

fun RespondToElicitation(renderer: DatastarFragmentRenderer) =
    "/elicitation/{action}/{id}" bind sse { sse ->
        sse.sendMergeFragments(renderer(EmptyElicitationForm))
        ElicitationResponse(
            Path.enum<ElicitationAction>().of("action")(sse.connectRequest),
            sse.datastarModel(),
            Meta(Path.of("id")(sse.connectRequest))
        )
        sse.close()
    }
