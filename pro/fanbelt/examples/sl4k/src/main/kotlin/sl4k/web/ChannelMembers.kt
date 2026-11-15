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

class ChannelMembers(private val workspace: Workspace, private val chat: ChatFeature) {
    private fun allows(request: Request) = try {
        workspace.scope(request).read(chat.visibleChannel, channelActorKey(request)) is Allowed
    } catch (_: LensFailure) {
        true
    }

    val requests = Filter { next -> { if (allows(it)) next(it) else Response(NOT_FOUND) } }
}
