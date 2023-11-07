package guide.howto.typesafe_your_api_with_lenses

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.format.Jackson.with
import org.http4k.format.Jackson
import org.http4k.format.invoke

data class Foo(val name: String)

fun main() {
    // and an item...
    val obj = Foo("hello")

    // we can use this alternative syntax to inject the value
    val messageWithJsonInjected = Request(GET, "").with(Jackson(obj))
    println(messageWithJsonInjected)

    // or we can use yet another alternative syntax to inject the value
    val messageWithJsonInjected2 = Request(GET, "").with(obj)
    println(messageWithJsonInjected2)

    // or to extract it again
    val extractedObject = Jackson<Foo>(messageWithJsonInjected)
    println(extractedObject)
}
