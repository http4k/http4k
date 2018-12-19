package guide.modules.hamkrest

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasQuery
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.string

fun main() {

    val request = Request(Method.POST, "/?a=b").body("http4k is cool").header("my header", "a value")

    // status
    Response(OK) shouldMatch hasStatus(OK)

    // query
    request shouldMatch hasQuery("a", "b")

    // header
    request shouldMatch hasHeader("my header", "a value")

    // body
    request shouldMatch hasBody(equalTo("http4k is cool"))
    request shouldMatch hasBody("http4k is cool")
    request shouldMatch hasBody(Body.string(ContentType.TEXT_HTML).toLens(), equalTo("http4k is cool"))

    // composite
    request shouldMatch hasBody(equalTo("http4k is cool")).and(hasQuery("a", "b"))
}
