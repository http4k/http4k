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

/**
 * A part of a page that shows one state for a key from the request. Its view is handed the state, whoever the page
 * admitted, and the key, and must render one root element with the region's id. If reading or rendering fails, it shows
 * its fallback and reports RedrawFailed.
 */
class Region<in Actor, Key : Any, Model>(
    id: String,
    private val renderer: TemplateRenderer,
    private val key: (Request) -> Key,
    private val state: State<Key, Model>,
    private val fallback: ViewModel? = null,
    private val view: (Model, Actor, Key) -> ViewModel,
) {
    val id = RegionId.of(id)

    companion object {
        /**
         * A region whose view needs only the state and its key, so it fits a page for any viewer.
         */
        operator fun <Key : Any, Model> invoke(id: String, renderer: TemplateRenderer, key: (Request) -> Key, state: State<Key, Model>, fallback: ViewModel? = null, view: (Model, Key) -> ViewModel): Region<Any?, Key, Model> =
            Region(id, renderer, key, state, fallback) { model: Model, _: Any?, key: Key -> view(model, key) }

        /**
         * A region of a state with no key, whose view is handed whoever the page admitted.
         */
        operator fun <Actor, Model> invoke(id: String, renderer: TemplateRenderer, state: State<Unit, Model>, fallback: ViewModel? = null, view: (Model, Actor) -> ViewModel): Region<Actor, Unit, Model> =
            Region(id, renderer, { _: Request -> }, state, fallback) { model: Model, actor: Actor, _: Unit -> view(model, actor) }

        /**
         * A region of a state with no key, whose view needs only the state.
         */
        operator fun <Model> invoke(id: String, renderer: TemplateRenderer, state: State<Unit, Model>, fallback: ViewModel? = null, view: (Model) -> ViewModel): Region<Any?, Unit, Model> =
            Region(id, renderer, { _: Request -> }, state, fallback) { model: Model, _: Any?, _: Unit -> view(model) }
    }

    private val shared = ConcurrentHashMap<Pair<TenantId, Key>, Shared>()

    private val unavailable by lazy { Rendered(unavailableHtml()) }

    private val root = Regex("""^\s*<[A-Za-z][\w-]*\b[^>]*\bid="${Regex.escape(this.id.value)}"""")

    internal fun keyParses(request: Request) = lensOrNull { key(request) } != null

    internal fun firstPaint(ctx: StateContext<*>, request: Request, actor: Actor, events: Events): FirstPaint =
        runCatching { key(request).let { key -> key to state.read(ctx, key) } }.fold(
            { (key, current) -> FirstPaint(current == null) { runCatching { render(view(current, actor, key)) }.getOrElse { failed(events, it) } } },
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

    internal fun watch(scope: Scope, request: Request, actor: Actor, send: (Rendered) -> Unit): Subscription {
        val key = key(request)
        val shareKey = scope.tenant to key
        val listener = Listener(actor, send)
        val entry = shared.compute(shareKey) { _, existing -> (existing ?: Shared(scope, key)).also { it.viewers.join(listener) } }!!
        if (!entry.viewers.catchUp(listener, entry::present)) entry.refresh()
        return Subscription {
            val remaining = shared.computeIfPresent(shareKey) { _, existing -> if (existing.viewers.leave(listener)) null else existing }
            if (remaining == null) entry.trace.close()
        }
    }

    private inner class Shared(scope: Scope, private val key: Key) {
        private val events = scope.events

        private val lock = ReentrantLock()

        val viewers: Viewers<@UnsafeVariance Actor, Model> = Viewers()

        val trace = Trace(scope, { presence -> scope.redraw({ events(RedrawWaited(id, it)) }) { scope.snapshot(presence) { ctx -> state.read(ctx, key) } } }, ::refresh)

        fun refresh(): Unit = lock.withLock {
            if (!viewers.watched) return
            runCatching { trace.read() }
                .onFailure { failure ->
                    events(RedrawFailed(id, failure))
                    viewers.unavailable(unavailable)
                }
                .onSuccess { current ->
                    val changed = viewers.differs(current)
                    events(Redrawn(id, key.takeIf { it != Unit }, changed))
                    if (changed) viewers.show(current, ::present)
                }
        }

        fun present(current: Model): (Actor) -> Rendered {
            val rendered = mutableMapOf<ViewModel, Result<Rendered>>()
            return { actor ->
                runCatching { view(current, actor, key) }.mapCatching { model -> rendered.getOrPut(model) { runCatching { Rendered(render(model)) } }.getOrThrow() }
                    .getOrElse { failure ->
                        events(RedrawFailed(id, failure))
                        unavailable
                    }
            }
        }
    }

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
