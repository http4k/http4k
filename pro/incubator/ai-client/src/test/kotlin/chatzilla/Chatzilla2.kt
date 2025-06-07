package chatzilla

import chatzilla.endpoints.ApproveTool
import chatzilla.endpoints.DenyTool
import chatzilla.endpoints.GetHistory
import chatzilla.endpoints.GetMessageForm
import chatzilla.endpoints.Index
import chatzilla.endpoints.SendUserMessage
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.template.DatastarFragmentRenderer
import org.http4k.template.HandlebarsTemplates

fun Chatzilla2(mcp: Uri): HttpHandler {
    val renderer = HandlebarsTemplates().CachingClasspath()
    val datastarRenderer = DatastarFragmentRenderer(renderer)

    val history = ChatHistory("Welcome to Chatzilla!")

    return ServerFilters.CatchAll().then(
        routes(
            GetMessageForm(datastarRenderer),
            SendUserMessage(history, datastarRenderer),
            ApproveTool(history, datastarRenderer),
            DenyTool(history, datastarRenderer),
            GetHistory(history, renderer),
            Index(renderer),
            static(Classpath("public")),
        )
    )
}

