package cookbook.custom_json_marshallers

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
// this import is important so you don't pick up the standard auto method!
import cookbook.custom_json_marshallers.MyJackson.auto
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.text
import org.http4k.format.withStandardMappings

object MyJackson : ConfigurableJackson(KotlinModule()
    .asConfigurable()
    .withStandardMappings()
    // declare custom mapping for our own types - this one represents our type as a simple String
    .text(::PublicType, PublicType::value)
    // ... and this one shows a masked value and cannot be deserialised (as the mapping is only one way)
    .text(SecretType::toString)
    .done()
    .deactivateDefaultTyping()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
)

data class PublicType(val value: String)
data class SecretType(val value: String) {
    override fun toString(): String {
        return "****"
    }
}

data class MyType(val public: PublicType, val hidden: SecretType)

fun main() {
    println(
        Response(OK).with(Body.auto<MyType>().toLens() of MyType(PublicType("hello"), SecretType("secret")))
    )

    /** Prints:

    HTTP/1.1 200 OK
    content-type: application/json; charset=utf-8

    {"public":"hello","hidden":"****"}

     */
}
