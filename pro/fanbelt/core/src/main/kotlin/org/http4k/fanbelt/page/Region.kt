/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import org.http4k.core.Request
import org.http4k.events.Events
import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.events.Redrawn
import org.http4k.fanbelt.input.lensOrNull
import org.http4k.fanbelt.state.State
import org.http4k.fanbelt.state.StateContext
import org.http4k.fanbelt.state.snapshot
import org.http4k.fanbelt.tenancy.Scope
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.security.Sha256
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Region<Key : Any, Model>(
    id: String,
    private val renderer: TemplateRenderer,
    private val key: (Request) -> Key,
    private val state: State<Key, Model>,
    private val fallback: ViewModel? = null,
    private val view: (Model, Request) -> ViewModel,
) {
    val id = RegionId.of(id)

    companion object {
        operator fun <Model> invoke(id: String, renderer: TemplateRenderer, state: State<Unit, Model>, fallback: ViewModel? = null, view: (Model, Request) -> ViewModel) =
            Region(id, renderer, { _: Request -> }, state, fallback, view)
    }

    private val shared = ConcurrentHashMap<Pair<TenantId, Key>, Shared>()

    private val unavailable by lazy { Rendered(unavailableHtml()) }

    private val root = Regex("""^\s*<[A-Za-z][\w-]*\b[^>]*\bid="${Regex.escape(this.id.value)}"""")

    internal fun keyParses(request: Request) = lensOrNull { key(request) } != null

    internal fun firstPaint(ctx: StateContext<*>, request: Request, events: Events): FirstPaint =
        runCatching { ctx.read(state, key(request)) }.fold(
            { current -> FirstPaint(current == null) { runCatching { html(current, request) }.getOrElse { failed(events, it) } } },
            { failure -> FirstPaint(false) { failed(events, failure) } },
        )

    private fun failed(events: Events, failure: Throwable): String {
        events(RedrawFailed(id, failure))
        return unavailable.html
    }

    private fun unavailableHtml(): String {
        val custom = fallback ?: return id.placeholder()
        return runCatching { render(custom) }.getOrDefault(id.placeholder())
    }

    internal fun watch(scope: Scope, request: Request, send: (Rendered) -> Unit): Subscription {
        val key = key(request)
        val shareKey = scope.tenant to key
        val listener = Listener(request, send)
        val entry = shared.compute(shareKey) { _, existing -> (existing ?: Shared(scope, key)).also { it.viewers.join(listener) } }!!
        if (!entry.viewers.catchUp(listener, entry::present)) entry.refresh()
        return Subscription {
            val remaining = shared.computeIfPresent(shareKey) { _, existing -> if (existing.viewers.leave(listener)) null else existing }
            if (remaining == null) entry.trace.close()
        }
    }

    private inner class Shared(scope: Scope, private val key: Key) {
        private val events = scope.events

        private val named = if (key == Unit) "" else key.toString()

        private val lock = ReentrantLock()

        val viewers = Viewers<Model>()

        val trace = Trace(scope, { presence -> scope.redraw({ events(RedrawWaited(id, it)) }) { scope.snapshot(presence) { ctx -> ctx.read(state, key) } } }, ::refresh)

        fun refresh(): Unit = lock.withLock {
            if (!viewers.watched) return
            runCatching { trace.read() }
                .onFailure { failure ->
                    events(RedrawFailed(id, failure))
                    viewers.unavailable(unavailable)
                }
                .onSuccess { current ->
                    val changed = viewers.differs(current)
                    events(Redrawn(id, named, changed))
                    if (changed) viewers.show(current, ::present)
                }
        }

        fun present(current: Model): (Request) -> Rendered {
            val rendered = mutableMapOf<ViewModel, Result<Rendered>>()
            return { request ->
                runCatching { view(current, request) }.mapCatching { model -> rendered.getOrPut(model) { runCatching { Rendered(render(model)) } }.getOrThrow() }
                    .getOrElse { failure ->
                        events(RedrawFailed(id, failure))
                        unavailable
                    }
            }
        }
    }

    private fun html(current: Model, request: Request) = render(view(current, request))

    private fun render(model: ViewModel): String {
        val html = renderer(model)
        check(root.containsMatchIn(html)) { "region $id must render one root element with id=\"$id\"" }
        return html
    }
}

internal class Rendered(val html: String) {
    internal val digest by lazy { digest(html) }
}

internal fun digest(html: String): String = Sha256.hash(html).take(16)

internal class FirstPaint(val absent: Boolean, val html: () -> String)
