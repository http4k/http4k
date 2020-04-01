package guide.modules.hamkrest

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasQuery
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.string

fun main() {

    val request = Request(POST, "/?a=b").body("http4k is cool").header("my header", "a value")

    // status
    assertThat(Response(OK), hasStatus(OK))

    // query
    assertThat(request, hasQuery("a", "b"))

    // header
    assertThat(request, hasHeader("my header", "a value"))

    // body
    assertThat(request, hasBody(equalTo("http4k is cool")))
    assertThat(request, hasBody("http4k is cool"))
    assertThat(request, hasBody(Body.string(ContentType.TEXT_HTML).toLens(), equalTo("http4k is cool")))

    // composite
    assertThat(request, hasBody(equalTo("http4k is cool")).and(hasQuery("a", "b")))
}
