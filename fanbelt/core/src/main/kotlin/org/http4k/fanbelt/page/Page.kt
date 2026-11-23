package org.http4k.fanbelt.page

import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.HttpHandler
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.fanbelt.feature.map
import org.http4k.fanbelt.input.DeclaredSignals
import org.http4k.fanbelt.input.PAINTED
import org.http4k.fanbelt.input.SignalsJson
import org.http4k.fanbelt.input.SignalsJson.datastarModel
import org.http4k.fanbelt.state.reading
import org.http4k.fanbelt.tenancy.Scope
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.format.asDatastarSignal
import org.http4k.routing.bind
import org.http4k.sse.Sse
import org.http4k.sse.SseHandler
import org.http4k.sse.SseMessage
import org.http4k.sse.SseResponse
import org.http4k.sse.sendPatchSignals
import org.http4k.template.viewModel
import java.time.Duration
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * A live page: its first paint and an SSE stream that redraws regions as they change. A region key that doesn't parse
 * is a 400; an absent subject is a 404.
 */
class Page private constructor(internal val http: HttpHandler, internal val sse: SseHandler) {
    companion object {
        /**
         * A page drawn by its layout for whoever its viewing lets in: the layout's shell is given what the rule found,
         * or the actor for a page with no rule.
         */
        operator fun <Found> invoke(workspace: Workspace, layout: Layout<Found>, viewing: Viewing<Found>) =
            LivePage(workspace, layout, viewing).let { Page(it.http, it.sse) }

        /**
         * A page anyone in the tenant may see.
         */
        operator fun invoke(workspace: Workspace, layout: Layout<Unit>) = invoke(workspace, layout, Viewing())
    }
}

private class LivePage<Found>(
    private val workspace: Workspace,
    private val layout: Layout<Found>,
    private val viewing: Viewing<Found>
) {
    private val runtime = workspace.infra.runtime

    val sse: SseHandler = sse@{ request ->
        if (!keysParse(request)) return@sse SseResponse(BAD_REQUEST) { it.close() }
        val scope = workspace.scope(request)
        val admission = viewing.admit(scope, request)
        when (val refused = admission.refused) {
            null -> SseResponse { sse ->
                val connection = Connection(sse)
                try {
                    stream(sse, connection, scope, request, admission)
                } catch (e: Exception) {
                    runCatching { sse.close() }
                    throw e
                }
            }

            else -> SseResponse(refused) { it.close() }
        }
    }

    private fun stream(sse: Sse, connection: Connection, scope: Scope, request: Request, admission: Admission) {
        val painted = painted(request)
        val outbox = Outbox(
            sse,
            runtime.scheduler,
            scope.events,
            painted,
            viewing.actor(request).takeIf { it != Unit }
        )
        connection.own(admission.open(outbox::leave))
        sse.send(SseMessage.Retry(Duration.ofMillis(500L + runtime.random.nextInt(4500))))
        if (painted.isNotEmpty()) sse.sendPatchSignals(SignalsJson.asDatastarSignal(mapOf(PAINTED to null)))
        layout.regions.forEach { region ->
            connection.own(region.watch(scope, request) { outbox.post(region.id, it) })
        }
        val pinging = runtime.scheduler.scheduleWithFixedDelay(
            outbox::ping,
            workspace.infra.keepAlive.toMillis(),
            workspace.infra.keepAlive.toMillis(),
            MILLISECONDS
        )
        connection.own { pinging.cancel(false) }
    }

    val http: HttpHandler = { request ->
        if (!keysParse(request)) {
            Response(BAD_REQUEST)
        } else {
            paint(request)
        }
    }

    private fun keysParse(request: Request) = layout.regions.all { it.keyParses(request) }

    private fun painted(request: Request) = runCatching { request.datastarModel<PaintedSignal>().fanbeltPainted.mapKeys { RegionId.of(it.key) } }.getOrElse { emptyMap() }

    private val html = Body.viewModel(layout.renderer, TEXT_HTML).toLens()

    private val declared = DeclaredSignals(layout.signals)

    private fun paint(request: Request): Response {
        val scope = workspace.scope(request)
        val drawn = scope.reading { ctx ->
            viewing.read(ctx, request).map { found -> found to layout.regions.associateWith { it.firstPaint(ctx, request, scope.events) } }
        }.value
        return viewing.answer(request, drawn) { (found, paints) ->
            val present = layout.subject == null || !paints.getValue(layout.subject).absent
            val htmls = paints.entries.associate { (region, paint) -> region.id.value to paint.html() }
            Response(if (present) OK else NOT_FOUND).with(
                html of layout.shell(request, PageShell(workspace.uri(request), Uri.of(request.uri.path).copy(query = request.uri.query), htmls, declared.json(htmls.mapValues { digest(it.value) })), found)
            )
        }
    }
}

private data class PaintedSignal(val fanbeltPainted: Map<String, String> = emptyMap())

/**
 * Mounts a page's first paint and stream on a route.
 */
infix fun String.bind(page: Page) = this bind PolyHandler(http = page.http, sse = page.sse)
