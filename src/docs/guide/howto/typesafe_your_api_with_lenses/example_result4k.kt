package guide.howto.typesafe_your_api_with_lenses

import com.fasterxml.jackson.databind.JsonNode
import dev.forkhandles.result4k.Result
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.format.Jackson.json
import org.http4k.lens.LensFailure
import org.http4k.lens.Query
import org.http4k.lens.asResult
import org.http4k.lens.int

fun main() {
    val queryResultLens = Query.int().required("foo").asResult()
    val intResult: Result<Int, LensFailure> = queryResultLens(Request(GET, "/?foo=123"))
    println(intResult)

    val jsonResultLens = Body.json().toLens().asResult()
    val jsonResult: Result<JsonNode, LensFailure> = jsonResultLens(Request(GET, "/foo"))
    println(jsonResult)
}
