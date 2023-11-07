package guide.howto.typesafe_your_api_with_lenses

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.format.invoke

data class Foo(val name: String)

fun main() {
    // given an HTTP message...
    val httpMessage = Request(GET, "")

    // and an item...
    val obj = Foo("hello")

    // we can use this alternative syntax to inject the value
    val injected = httpMessage.with(Jackson(obj))

    println(injected)

    // or to extract it again
    val extracted = Jackson<Foo>(injected)

    println(extracted)
}
