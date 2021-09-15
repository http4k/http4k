package guide.reference.templating

import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel
import java.io.File

data class Person(val name: String, val age: Int) : ViewModel

fun main() {

    // first, create a Renderer - this can be a Caching instance or a HotReload for development
    val renderer = HandlebarsTemplates().HotReload("src/test/resources")

    // first example uses a renderer to create a string
    val app: HttpHandler = {
        val viewModel = Person("Bob", 45)
        val renderedView = renderer(viewModel)
        Response(OK).body(renderedView)
    }
    println(app(Request(GET, "/someUrl")))

    // the lens example uses the Body.viewModel to also set the content type, and avoid using Strings
    val viewLens = Body.viewModel(renderer, TEXT_HTML).toLens()

    val appUsingLens: HttpHandler = {
        Response(OK).with(viewLens of Person("Bob", 45))
    }

    println(appUsingLens(Request(GET, "/someUrl")))

    // overwrite the content - this will prove the hot reload works!
    File("src/test/resources/guide.reference/templating/Person.hbs").writer()
        .use { it.write("{{name}} is not {{age}} years old") }

    println(appUsingLens(Request(GET, "/someUrl")))
}
