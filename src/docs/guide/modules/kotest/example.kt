package guide.modules.kotest

import io.kotest.matchers.be
import io.kotest.matchers.should
import io.kotest.matchers.string.startWith
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.kotest.haveBody
import org.http4k.kotest.haveHeader
import org.http4k.kotest.haveQuery
import org.http4k.kotest.haveStatus
import org.http4k.kotest.shouldHaveBody
import org.http4k.kotest.shouldHaveHeader
import org.http4k.kotest.shouldHaveQuery
import org.http4k.kotest.shouldHaveStatus
import org.http4k.lens.string

fun main() {

    val request = Request(POST, "/?a=b").body("http4k is cool").header("my header", "a value")

    // status
    Response(OK) should haveStatus(OK)
    Response(OK) shouldHaveStatus OK

    // query
    request should haveQuery("a", "b")
    request.shouldHaveQuery("a", "b")

    // header
    request should haveHeader("my header", "a value")
    request.shouldHaveHeader("my header", "a value")

    // body
    request should haveBody(startWith("http4k is cool"))
    request should haveBody("http4k is cool")
    request should haveBody(Body.string(ContentType.TEXT_HTML).toLens(), be("http4k is cool"))
    request shouldHaveBody "http4k is cool"

    // composite
    request should (haveQuery("a", "b") and haveBody("http4k is cool"))
}
