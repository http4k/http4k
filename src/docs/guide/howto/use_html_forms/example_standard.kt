package guide.howto.use_html_forms

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.body.form
import org.http4k.core.getFirst
import org.http4k.core.toParametersMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull

fun main() {
    // form(name: String, value: String?) parses the request body on each invocation
    val request = Request(GET, "/").form("name", "rita").form("age", "55")

    // form(vararg formData: Pair<String, String>) allows you to add multiple form fields to
    // the request while only parsing the request body once
    val allInOneRequest = Request(GET, "/").form("name" to "rita", "age" to "55")

    // form(name: String) parses the request body on each invocation
    assertEquals("rita", request.form("name"))
    assertEquals("55", request.form("age"))
    assertNull(request.form("height"))

    assertEquals("rita", allInOneRequest.form("name"))
    assertEquals("55", allInOneRequest.form("age"))
    assertNull(allInOneRequest.form("height"))

    // toParametersMap() gives form as map
    val parameters: Map<String, List<String?>> = request.form().toParametersMap()
    assertEquals("rita", parameters.getFirst("name"))
    assertEquals(listOf("55"), parameters["age"])
    assertNull(parameters["height"])
}
