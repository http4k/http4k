package cookbook

import org.reekwest.http.core.Request
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response.Companion.ok
import org.reekwest.http.core.then
import org.reekwest.http.core.with
import org.reekwest.http.filters.ServerFilters
import org.reekwest.http.lens.Body
import org.reekwest.http.lens.Header
import org.reekwest.http.lens.Query
import org.reekwest.http.lens.int

fun main(args: Array<String>) {

    data class Child(val name: String)

    val nameQuery = Header.required("name")
    val ageQuery = Query.int().optional("age")
    val childrenBody = Body.string.map({ it.split(",").map(::Child) }, { it.map { it.name }.joinToString() }).required()

    val endpoint = {
        request: Request ->

        val name: String = nameQuery(request)
        val age: Int? = ageQuery(request)
        val children: List<Child> = childrenBody(request)

        val msg = "$name is ${age ?: "unknown"} years old and has " +
            "${children.size} children (${children.map { it.name }.joinToString()})"
        ok().with(
            Body.string.required() to msg
        )
    }

    val app = ServerFilters.CatchLensFailure.then(endpoint)

    val goodRequest = get("http://localhost:9000").header("name", "Jane Doe").query("age", "25").body("rita,sue,bob")

    println(listOf("", "Request:", goodRequest, app(goodRequest)).joinToString("\n"))

    val badRequest = get("http://localhost:9000").header("name", "John Doe").query("age", "too old!")

    println(listOf("", "Request:", badRequest, app(badRequest)).joinToString("\n"))
}
