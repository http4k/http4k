package cookbook

import org.reekwest.http.core.Request
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response.Companion.ok
import org.reekwest.http.core.then
import org.reekwest.http.core.with
import org.reekwest.http.lens.Body
import org.reekwest.http.lens.CatchLensFailure
import org.reekwest.http.lens.Header
import org.reekwest.http.lens.Query
import org.reekwest.http.lens.int

fun main(args: Array<String>) {

    val nameQuery = Header.required("name")
    val ageQuery = Query.int().required("age")

    val endpoint = { request: Request ->

        val name: String = nameQuery(request)
        val age: Int = ageQuery(request)

        ok().with(
            Body.string.required() to "$name is $age years old"
        )
    }

    val app = CatchLensFailure.then(endpoint)

    val goodRequest = get("http://localhost:9000").header("name", "Jane Doe").query("age", "25")

    println(listOf("","Request:", goodRequest, app(goodRequest)).joinToString("\n"))

    val badRequest = get("http://localhost:9000").header("name", "John Doe").query("age", "too old!")

    println(listOf("","Request:", badRequest, app(badRequest)).joinToString("\n"))
}
