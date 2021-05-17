package guide.howto.html_forms

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.body.form
import org.http4k.core.getFirst
import org.http4k.core.toParametersMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull

fun main() {
    val request = Request(GET, "/").form("name", "rita").form("age", "55")

    // reparses body every invocation
    assertEquals("rita", request.form("name"))
    assertEquals("55", request.form("age"))
    assertNull(request.form("height"))

    // toParametersMap() gives form as map
    val parameters: Map<String, List<String?>> = request.form().toParametersMap()
    assertEquals("rita", parameters.getFirst("name"))
    assertEquals(listOf("55"), parameters["age"])
    assertNull(parameters["height"])
}
