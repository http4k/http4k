package sl4k.web

import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.fanbelt.feature.Allowed
import org.http4k.fanbelt.feature.ActorKey
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.lens.LensFailure
import sl4k.feature.ChatFeature

val channelActorKey = { request: Request -> ActorKey(principal(request), channelName(request)) }

fun ChannelMembers(workspace: Workspace, chat: ChatFeature) = Filter { next ->
    { request -> if (allows(workspace, chat, request)) next(request) else Response(NOT_FOUND) }
}

private fun allows(workspace: Workspace, chat: ChatFeature, request: Request) = try {
    workspace.scope(request).read(chat.visibleChannel, channelActorKey(request)) is Allowed
} catch (_: LensFailure) {
    true
}
