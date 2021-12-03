package guide.howto.typesafe_your_api_with_lenses

import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.lens.Header
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.composite
import org.http4k.lens.int
import org.http4k.lens.string

fun main() {

    data class Child(val name: String)
    data class Pageable(val sortAscending: Boolean, val page: Int, val maxResults: Int)

    val nameHeader = Header.required("name")
    val ageQuery = Query.int().optional("age")
    val childrenBody = Body.string(TEXT_PLAIN).map({ it.split(",").map(::Child) }, { it.joinToString { it.name } }).toLens()
    val pageable = Query.composite {
        Pageable(
            boolean().defaulted("sortAscending", true)(it),
            int().defaulted("page", 1)(it),
            int().defaulted("maxResults", 20)(it)
        )
    }

    val endpoint = { request: Request ->

        val name: String = nameHeader(request)
        val age: Int? = ageQuery(request)
        val children: List<Child> = childrenBody(request)
        val pagination = pageable(request)

        val msg = """
$name is ${age ?: "unknown"} years old and has 
${children.size} children (${children.joinToString { it.name }})
Pagination: $pagination
            """
        Response(OK).with(
            Body.string(TEXT_PLAIN).toLens() of msg
        )
    }

    val app = ServerFilters.CatchLensFailure.then(endpoint)

    val goodRequest = Request(GET, "http://localhost:9000").with(
        nameHeader of "Jane Doe",
        ageQuery of 25,
        childrenBody of listOf(Child("Rita"), Child("Sue")))

    println(listOf("", "Request:", goodRequest, app(goodRequest)).joinToString("\n"))

    val badRequest = Request(GET, "http://localhost:9000")
        .with(nameHeader of "Jane Doe")
        .query("age", "some illegal age!")

    println(listOf("", "Request:", badRequest, app(badRequest)).joinToString("\n"))
}
