package guide.modules.templating

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel
import org.http4k.template.view

data class Person(val name: String, val age: Int) : ViewModel

fun main(args: Array<String>) {

    // first, create a Renderer - this can be a Caching instance or a HotReload for development
    val renderer = HandlebarsTemplates().HotReload("src/test/resources")

    // OR we can customise the Handlebars instance here...
    val rendererWithConfig = HandlebarsTemplates { handlebars: Handlebars ->
        // configure handlebars here...
        handlebars
    }.CachingClasspath("src/test/resources")

    // first example uses a renderer to create a string
    val app: HttpHandler = {
        val viewModel = Person("Bob", 45)
        val renderedView = renderer(viewModel)
        Response(OK).body(renderedView)
    }
    println(app(Request(Method.GET, "/someUrl")))

    // the lens example uses the Body.view to also set the content type, and avoid using Strings
    val viewLens = Body.view(renderer, ContentType.TEXT_HTML)

    val appUsingLens: HttpHandler = {
        Response(OK).with(viewLens of Person("Bob", 45))
    }

    println(appUsingLens(Request(Method.GET, "/someUrl")))
}