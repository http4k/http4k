package cookbook.typesafe_http_requests_with_lenses

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Method.*
import org.http4k.core.Request
import org.http4k.format.Jackson.json
import org.http4k.lens.LensExtractor
import org.http4k.lens.LensFailure
import org.http4k.lens.Query
import org.http4k.lens.int

// This is our custom Result/Either ADT, although it could be anything, like a Result4k Result (which has map() etc)
sealed class Result<out T>

data class Succeeded<out T>(val value: T) : Result<T>()
data class Failed<out T>(val e: Exception) : Result<T>()


// This simple extension method can be used to convert all Lenses to return our custom Result type instead of the standard exception
fun <IN, OUT> LensExtractor<IN, OUT>.toResult(): LensExtractor<IN, Result<OUT>> = object : LensExtractor<IN, Result<OUT>> {
    override fun invoke(target: IN): Result<OUT> = try {
        Succeeded(this@toResult.invoke(target))
    } catch (e: LensFailure) {
        Failed(e)
    }
}

// examples of using the above extension function
fun main() {

    val queryResultLens = Query.int().required("foo").toResult()
    val intResult: Result<Int> = queryResultLens(Request(GET, "/?foo=123"))

    println(intResult)
    val jsonResultLens = Body.json().toLens().toResult()
    val jsonResult: Result<JsonNode> = jsonResultLens(Request(GET, "/foo"))

    println(jsonResult)

}
