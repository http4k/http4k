package cookbook

import org.http4k.http.core.HttpHandler
import org.http4k.http.core.Request.Companion.get
import org.http4k.http.core.Response
import org.http4k.http.core.Status.Companion.OK
import org.http4k.http.templates.HandlebarsTemplates
import org.http4k.http.templates.ViewModel

// this view has the default template path of: cookbook/Person.hbs, although that is overridable
data class Person(val name: String, val age: Int) : ViewModel

fun main(args: Array<String>) {

    val renderer = HandlebarsTemplates().HotReload("src/test/resources")

    val app: HttpHandler = {
        val viewModel = Person("Bob", 45)
        val renderedView = renderer(viewModel)
        Response(OK).body(renderedView)
    }

    println(app(get("/someUrl")))
}

