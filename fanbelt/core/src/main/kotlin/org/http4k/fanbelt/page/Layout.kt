package org.http4k.fanbelt.page

import org.http4k.fanbelt.tenancy.Scope
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel

/**
 * What a page draws: its regions, signals and view.
 */
class Layout<in Actor, in Key, in Found>(
    internal val regions: List<Region<Actor, *, *>>,
    internal val renderer: TemplateRenderer,
    internal val subject: Region<Actor, *, *>? = null,
    internal val signals: Any? = null,
    internal val view: (Scope, PageShell, Admitted<Actor, Key, Found>) -> ViewModel,
) {
    /**
     * For a view that needs no scope.
     */
    constructor(
        regions: List<Region<Actor, *, *>>,
        renderer: TemplateRenderer,
        subject: Region<Actor, *, *>? = null,
        signals: Any? = null,
        view: (PageShell, Admitted<Actor, Key, Found>) -> ViewModel,
    ) : this(regions, renderer, subject, signals, { _, shell, admitted -> view(shell, admitted) })

    init {
        require(subject == null || subject in regions) { "A layout's subject must be one of its regions: ${subject?.id}" }
    }
}

/**
 * For a view that needs only the shell.
 */
fun <Actor> Layout(
    regions: List<Region<Actor, *, *>>,
    renderer: TemplateRenderer,
    subject: Region<Actor, *, *>? = null,
    signals: Any? = null,
    view: (PageShell) -> ViewModel,
): Layout<Actor, Any?, Any?> = Layout(regions, renderer, subject, signals) { shell, _: Admitted<Actor, Any?, Any?> -> view(shell) }
