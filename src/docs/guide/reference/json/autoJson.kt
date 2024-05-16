package guide.reference.json

import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.http4k.format.Jackson.json

data class Email(val value: String)
data class Message(val subject: String, val from: Email, val to: Email)

fun main() {
    // We can use the auto method here from either Moshi, Jackson ... message format objects.
    // Note that the auto() method needs to be manually imported as IntelliJ won't pick it up automatically.
    val messageLens = Body.auto<Message>().toLens()

    val myMessage = Message("hello", Email("bob@git.com"), Email("sue@git.com"))

    /**
     * There are several options for injection/extraction API:
     */

    // 1. Lens-first approach

    // to inject the body into the message apply the lens with the "part" - this also works with Response
    val requestWithEmail = messageLens(myMessage, Request(GET, "/"))

    println(requestWithEmail)

// Produces:
//    GET / HTTP/1.1
//    content-type: application/json
//
//    {"subject":"hello","from":{"value":"bob@git.com"},"to":{"value":"sue@git.com"}}

    // to extract the body from the message apply the lens - this also works with Response
    val extractedMessage = messageLens(requestWithEmail)

    println(extractedMessage)
    println(extractedMessage == myMessage)

// Produces:
//    Message(subject=hello, from=Email(value=bob@git.com), to=Email(value=sue@git.com))
//    true

    // 2. with()/of() approach - this reuses the lense
    val requestWithEmail2 = Request(GET, "/").with(messageLens of myMessage)

    println(requestWithEmail2)

    // 3. json() approach - user friendly but recreates the lense with every call
    val requestWithEmail3 = Request(GET, "/").json(myMessage)

    println(requestWithEmail3)

    val extractedMessage2 = requestWithEmail3.json<Message>()

    println(extractedMessage2)
}
