package tutorials.self_documenting_apis_with_openapi

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.hamkrest.hasBody
import org.junit.jupiter.api.Test

class GreetingRouteTest {
    @Test
    fun `greets an adult`() {
        assertThat(basicRoute(Request(GET, "/greet/Bob/21")), hasBody("Hello Bob, would you like some beer?"))
    }
}