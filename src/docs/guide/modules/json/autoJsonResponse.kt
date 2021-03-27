import kotlinx.serialization.Serializable // could use one of many json libraries: Gson, Jackson, Moshi, etc.
import org.http4k.core.*
import org.http4k.format.KotlinxSerialization.auto
import org.http4k.lens.BiDiBodyLens

@Serializable // required by Kotlinx.Serialization
data class Car(val brand: String, val model: String, val year: Int, val miles: Int)

// 'auto' is an extension function of each org.http4k.format.[serialization library]
// example: https://github.com/http4k/http4k/blob/master/http4k-format/kotlinx-serialization/src/main/kotlin/org/http4k/format/ConfigurableKotlinxSerialization.kt
val lensCarResponse : BiDiBodyLens<Car> = Body.auto<Car>().toLens() // BiDi allows for outgoing + incoming

fun main() {

    val sweetride = Car("Porsche", "911 Turbo", 1988, 45000)

      // lens.inject(object, response) serializes the object and sets content-type header to 'application/json'
      // can be used with any Serializable type (Map, List, etc)
    val app: HttpHandler = { request: Request -> lensCarResponse.inject(sweetride, Response(Status.OK)) }

    val request: Request = Request(Method.GET, "/")
    val response = app(request)

    println(response)
    /* 
    HTTP/1.1 200 OK
    content-type: application/json; charset=utf-8

    {"brand":"Porsche","model":"911 Turbo","year":1988,"miles":45000}
    */
}
