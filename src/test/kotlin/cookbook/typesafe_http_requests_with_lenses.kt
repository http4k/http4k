package cookbook

import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.lens.Header
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.string

fun main(args: Array<String>) {

    data class Child(val name: String)

    val nameQuery = Header.required("name")
    val ageQuery = Query.int().optional("age")
    val childrenBody = Body.string(TEXT_PLAIN).map({ it.split(",").map(::Child) }, { it.map { it.name }.joinToString() }).required()

    val endpoint = {
        request: Request ->

        val name: String = nameQuery(request)
        val age: Int? = ageQuery(request)
        val children: List<Child> = childrenBody(request)

        val msg = "$name is ${age ?: "unknown"} years old and has " +
            "${children.size} children (${children.map { it.name }.joinToString()})"
        Response(Status.OK).with(
            Body.string(TEXT_PLAIN).required() to msg
        )
    }

    val app = ServerFilters.CatchLensFailure.then(endpoint)

    val goodRequest = Request(Method.GET, "http://localhost:9000").header("name", "Jane Doe").query("age", "25").body("rita,sue,bob")

    println(listOf("", "Request:", goodRequest, app(goodRequest)).joinToString("\n"))

    val badRequest = Request(Method.GET, "http://localhost:9000").header("name", "John Doe").query("age", "too old!")

    println(listOf("", "Request:", badRequest, app(badRequest)).joinToString("\n"))
}
