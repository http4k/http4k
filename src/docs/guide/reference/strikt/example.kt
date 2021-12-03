package guide.reference.strikt

import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.strikt.body
import org.http4k.strikt.bodyString
import org.http4k.strikt.header
import org.http4k.strikt.query
import org.http4k.strikt.status
import strikt.api.expectThat
import strikt.assertions.isEqualTo

fun main() {

    val request = Request(POST, "/?a=b").body("http4k is cool").header("my header", "a value")

    // status
    expectThat(Response(OK)).status.isEqualTo(OK)

    // query
    expectThat(request).query("a").isEqualTo("b")

    // header
    expectThat(request).header("my header").isEqualTo("a value")

    // body
    expectThat(request).bodyString.isEqualTo("http4k is cool")
    expectThat(request).body.isEqualTo(Body("http4k is cool"))

    // composite
    expectThat(request) {
        bodyString.isEqualTo("http4k is cool")
        query("a").isEqualTo("b")
    }
}
