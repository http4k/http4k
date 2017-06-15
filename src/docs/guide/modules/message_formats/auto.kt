package guide.modules.message_formats

import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.format.Jackson.auto

data class Email(val value: String)
data class Message(val subject: String, val from: Email, val to: Email)

fun main(args: Array<String>) {
    val messageLens = Body.auto<Message>().toLens()

    val myMessage = Message("hello", Email("bob@git.com"), Email("sue@git.com"))

    // to inject the body into the message - this also works with Response
    val requestWithEmail = messageLens.inject(myMessage, Request(GET, "/"))

    println(requestWithEmail)

// Produces:
//    GET / HTTP/1.1
//    content-type: application/json
//
//    {"subject":"hello","from":{"value":"bob@git.com"},"to":{"value":"sue@git.com"}}

    // to extract the body from the message - this also works with Response
    val extractedMessage = messageLens.extract(requestWithEmail)

    println(extractedMessage)
    println(extractedMessage == myMessage)

// Produces:
//    Message(subject=hello, from=Email(value=bob@git.com), to=Email(value=sue@git.com))
//    true
}
