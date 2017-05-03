package cookbook

import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.templates.HandlebarsTemplates
import org.reekwest.http.templates.ViewModel

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

