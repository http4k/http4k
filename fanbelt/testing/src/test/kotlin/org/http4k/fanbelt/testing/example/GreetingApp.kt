package org.http4k.fanbelt.testing.example

import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.cookie.Cookie
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.infrastructure.Node
import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.PageShell
import org.http4k.fanbelt.page.Region
import org.http4k.fanbelt.page.bind
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.lens.Cookies
import org.http4k.routing.bind
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel

fun GreetingApp(runtime: AppRuntime = AppRuntime()): Node {
    val infra = AppInfra(runtime, InMemoryDocumentStorage())
    val workspace = Workspace(infra, "/{board}") { true }

    val greeting = Region("greeting", renderer, GreetingFeature.greeting) { greeting, request -> GreetingView(greeting, person(request)) }
    val page = Page(workspace, Layout(listOf(greeting), renderer, signals = GreetingSignals()) { _, shell -> GreetingPageView(shell) })

    val actions = routes(
        "/say" bind POST to GreetingFeature.say(workspace, person, { signals: GreetingSignals, _ -> signals.text }, reset = setOf(GreetingSignals::text)),
        "/clear" bind POST to GreetingFeature.clear(workspace, person) { },
    )

    return Node(workspace.routes(poly(listOf(actions), "/" bind page)), runtime, infra)
}

fun personCookie(name: PersonName) = Cookie("person", name.value, path = "/")

private val personLens = Cookies.required("person")

private val person = { request: Request -> PersonName.of(personLens(request).value) }

data class GreetingSignals(val text: String = "")

data class GreetingView(val greeting: Greeting?, val viewer: PersonName) : ViewModel

data class GreetingPageView(val shell: PageShell) : ViewModel

private val renderer: TemplateRenderer = { model ->
    when (model) {
        is GreetingPageView -> page(model.shell)
        is GreetingView -> greeting(model)
        else -> error("no template for $model")
    }
}

private fun page(shell: PageShell) = """<!doctype html>
<html>
<head>
  <base href="${shell.base}">
  <script type="module" src="https://cdn.jsdelivr.net/gh/starfederation/datastar@v1.0.4/bundles/datastar.js"></script>
</head>
<body data-init="${shell.connect.escaped()}" data-signals="${shell.signals.escaped()}">
  <form data-on:submit="@post('say')">
    <input class="text" data-bind:text aria-label="Greeting">
    <p class="error" data-show="${'$'}errors.text" data-text="${'$'}errors.text"></p>
  </form>
  ${shell.regions.getValue("greeting")}
</body>
</html>"""

private fun greeting(view: GreetingView) = when (val greeting = view.greeting) {
    null -> """<div id="greeting"></div>"""
    else -> """<div id="greeting"><p class="text">${greeting.text.escaped()}</p>${clearButton(greeting, view.viewer)}</div>"""
}

private fun clearButton(greeting: Greeting, viewer: PersonName) =
    if (greeting.by == viewer) """<button class="clear" data-on:click="@post('clear')">Clear</button>""" else ""

private fun String.escaped() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
