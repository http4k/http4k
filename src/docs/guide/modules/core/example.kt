package guide.modules.core

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.lens.Header
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.localDate
import org.http4k.lens.nonEmptyString
import org.http4k.lens.string
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.LocalDate

val pathLocalDate = Path.localDate().of("date")
val requiredQuery = Query.required("myQueryName")
val nonEmptyQuery = Query.nonEmptyString().required("myNonEmptyQuery")
val optionalHeader = Header.int().optional("Content-Length")
val responseBody = Body.string(ContentType.TEXT_PLAIN).toLens()

// Most of the useful common JDK types are covered. However, if we want to use our own types, we can just use `map()`
data class CustomType(val value: String)

val requiredCustomQuery = Query.map(::CustomType, { it.value }).required("myCustomType")

// To use the Lens, simply `invoke() or extract()` it using an HTTP message to extract the value, or alternatively `invoke() or inject()` it with the value if we are modifying (via copy) the message:
val handler: RoutingHttpHandler = routes(
    "/hello/{date:.*}" bind GET to { request: Request ->
        val pathDate: LocalDate = pathLocalDate(request)
        // SAME AS:
        // val pathDate: LocalDate = pathLocalDate.extract(request)

        val customType: CustomType = requiredCustomQuery(request)
        val anIntHeader: Int? = optionalHeader(request)

        val baseResponse = Response(OK)
        val responseWithHeader = optionalHeader(anIntHeader, baseResponse)
        // SAME AS:
        // val responseWithHeader = optionalHeader.inject(anIntHeader, baseResponse)

        responseBody("you sent $pathDate and $customType", responseWithHeader)
    }
)

// With the addition of the `CatchLensFailure` filter, no other validation is required when using Lenses, as http4k will handle invalid requests by returning a BAD_REQUEST (400) response.
val app = ServerFilters.CatchLensFailure.then(handler)(Request(GET, "/hello/2000-01-01?myCustomType=someValue"))

// More conveniently for construction of HTTP messages, multiple lenses can be used at once to modify a message, which is useful for properly building both requests and responses in a typesafe way without resorting to string values (especially in URLs which should never be constructed using String concatenation):
val modifiedRequest: Request = Request(GET, "http://google.com/{pathLocalDate}").with(
    pathLocalDate of LocalDate.now(),
    requiredQuery of "myAmazingString",
    optionalHeader of 123
)
