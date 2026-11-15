/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import java.time.Duration
import java.util.concurrent.TimeUnit.MILLISECONDS
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.HttpHandler
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.datastar.Element
import org.http4k.datastar.goTo
import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.feature.Access
import org.http4k.fanbelt.feature.refusal
import org.http4k.fanbelt.feature.status
import org.http4k.fanbelt.input.DeclaredSignals
import org.http4k.fanbelt.input.PAINTED
import org.http4k.fanbelt.input.SignalsJson
import org.http4k.fanbelt.input.SignalsJson.datastarModel
import org.http4k.fanbelt.state.StateContext
import org.http4k.fanbelt.state.reading
import org.http4k.fanbelt.state.snapshot
import org.http4k.fanbelt.tenancy.Scope
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.format.asDatastarSignal
import org.http4k.lens.html
import org.http4k.routing.bind
import org.http4k.sse.SseHandler
import org.http4k.sse.SseMessage
import org.http4k.sse.SseResponse
import org.http4k.sse.sendPatchSignals
import org.http4k.template.viewModel

/**
 * A live page: its first paint and an SSE stream that redraws regions as they change. A region key that doesn't parse
 * is a 400; an absent subject is a 404.
 */
class Page(private val workspace: Workspace, private val layout: Layout, private val viewing: Viewing = Viewing()) {
    private val runtime = workspace.infra.runtime

    internal val sse: SseHandler = sse@{ request ->
        if (!keysParse(request)) return@sse SseResponse(BAD_REQUEST) { it.close() }
        val scope = workspace.scope(request)
        val admission = viewing.access?.let { Admission(scope, request, it) }
        val refused = admission?.refused
        if (refused != null) {
            SseResponse(refused) { it.close() }
        } else {
            SseResponse { sse ->
                sse.send(SseMessage.Retry(Duration.ofMillis(500L + runtime.random.nextInt(4500))))
                val connected = viewing.onConnect(scope, request)
                val painted = painted(request)
                if (painted.isNotEmpty()) sse.sendPatchSignals(SignalsJson.asDatastarSignal(mapOf(PAINTED to null)))
                val outbox = Outbox(sse, runtime.scheduler, scope.events, painted, viewing.actor(request).takeIf { it != Unit }?.toString())
                val watches = layout.regions.map { region ->
                    region.watch(scope, request) { outbox.post(region.id, it) }
                } + listOfNotNull(admission?.onRefused { outbox.leave(leaving(request)) })
                val pinging = runtime.scheduler.scheduleWithFixedDelay(outbox::ping, workspace.infra.keepAlive.toMillis(), workspace.infra.keepAlive.toMillis(), MILLISECONDS)
                sse.onClose {
                    pinging.cancel(false)
                    watches.forEach(Subscription::close)
                    connected.close()
                }
            }
        }
    }

    internal val http: HttpHandler = { request ->
        if (!keysParse(request)) {
            Response(BAD_REQUEST)
        } else {
            paint(request)
        }
    }

    private fun keysParse(request: Request) = layout.regions.all { it.keyParses(request) }

    private fun refusal(ctx: StateContext<Unit>, request: Request) = viewing.access?.invoke(ctx, request)?.refusal()

    private fun leaving(request: Request) = Element.goTo(viewing.leaveTo(request))

    private fun painted(request: Request) = runCatching { request.datastarModel<PaintedSignal>().fanbeltPainted.mapKeys { RegionId.of(it.key) } }.getOrElse { emptyMap() }

    private val html = Body.viewModel(layout.renderer, TEXT_HTML).toLens()

    private val declared = DeclaredSignals(layout.signals)

    private fun paint(request: Request): Response {
        val scope = workspace.scope(request)
        val (refused, paints) = scope.reading { ctx ->
            refusal(ctx, request)?.let { it to emptyMap() } ?: (null to layout.regions.associateWith { it.firstPaint(ctx, request, scope.events) })
        }.value
        if (refused != null) return Response(refused).html(leaving(request).value)
        val found = layout.subject == null || !paints.getValue(layout.subject).absent
        val htmls = paints.entries.associate { (region, paint) -> region.id.value to paint.html() }
        return Response(if (found) OK else NOT_FOUND).with(
            html of layout.shell(request, PageShell(workspace.uri(request), Uri.of(request.uri.path).copy(query = request.uri.query), htmls, declared.json(htmls.mapValues { digest(it.value) })))
        )
    }
}


/**
 * Reads a viewer's access as their stream opens, and keeps reading it while the stream is open: refused is the status
 * they are refused with, and onRefused is called once it stops allowing them.
 */
private class Admission(scope: Scope, request: Request, access: (StateContext<Unit>, Request) -> Access<*>) : Subscription {
    private val trace = Trace(scope, { presence -> scope.snapshot(presence) { ctx -> access(ctx, request) } }, ::check)

    @Volatile
    private var leave: () -> Unit = {}

    @Volatile
    var refused: Status? = null
        private set

    init {
        check()
    }

    fun onRefused(leave: () -> Unit) = apply {
        this.leave = leave
        if (refused != null) leave()
    }

    private fun check() {
        refused = trace.read().refusal()
        if (refused != null) {
            close()
            leave()
        }
    }

    override fun close() = trace.close()
}

private data class PaintedSignal(val fanbeltPainted: Map<String, String> = emptyMap())

/**
 * Mounts a page's first paint and stream on a route.
 */
infix fun String.bind(page: Page) = this bind PolyHandler(http = page.http, sse = page.sse)
