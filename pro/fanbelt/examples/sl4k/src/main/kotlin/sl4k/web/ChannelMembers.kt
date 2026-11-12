package sl4k.web

import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.lens.LensFailure
import org.http4k.fanbelt.http.Guard
import org.http4k.fanbelt.feature.Access.Allowed
import org.http4k.fanbelt.feature.Attempt
import org.http4k.fanbelt.tenancy.Workspace
import sl4k.feature.ChatFeature

val channelAttempt = { request: Request -> Attempt(principal(request), channelName(request)) }

class ChannelMembers(private val workspace: Workspace, private val chat: ChatFeature) {
    private fun allows(request: Request) = try {
        workspace.scope(request).read(chat.visibleChannel, channelAttempt(request)) is Allowed
    } catch (_: LensFailure) {
        true
    }

    val requests = Filter { next -> { if (allows(it)) next(it) else Response(NOT_FOUND) } }

    val pages = Guard({ Response(NOT_FOUND) }) { request -> request.takeIf(::allows) }
}
