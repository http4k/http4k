package cookbook

import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel
import org.http4k.template.view

// this view has the default template path of: cookbook/Person.hbs, although that is overridable by
// setting the template property from ViewModel
data class Person(val name: String, val age: Int) : ViewModel

/**
 * Example showing how to use the Templating modules - in this case Handlebars, both by standard
 * response manipulation and via a typesafe view lens
 */
fun main(args: Array<String>) {

    val renderer = HandlebarsTemplates().HotReload("src/test/resources")

    val view = Body.view(renderer, TEXT_HTML)

    val app: HttpHandler = {
        val viewModel = Person("Bob", 45)
        Response(OK).body(renderer(viewModel))
        // OR:
        Response(OK).with(view of viewModel)
    }

    println(app(Request(Method.GET, "/someUrl")))
}

