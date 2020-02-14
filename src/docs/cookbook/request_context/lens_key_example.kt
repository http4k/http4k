package cookbook.request_context

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.*
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.lens.RequestContextKey
import org.http4k.lens.RequestContextLens


fun main() {
    data class SharedState(val message: String)


    fun AddState(key: RequestContextLens<SharedState>) = Filter { next ->
        {
            // "modify" the request like any other Lens
            next(it.with(key of SharedState("hello there")))
        }
    }

    fun PrintState(key: RequestContextLens<SharedState>): HttpHandler = { request ->
        // we can just extract the Lens state from the request like any other standard Lens
        println(key(request))
        Response(Status.OK)
    }

    // this is the shared RequestContexts object - it holds the bag of state for each request and
    // tidies up afterwards.
    val contexts = RequestContexts()

    // this Lens is the key we use to set and get the type-safe state. By using this, we gain
    // typesafety and the guarantee that there will be no clash of keys.
    // RequestContextKeys can be required, optional, or defaulted, as per the standard Lens mechanism.
    val key = RequestContextKey.required<SharedState>(contexts)

    // The first Filter is required to initialise the bag of state.
    // The second Filter modifies the bag.
    // The handler just prints out the state.
    val app = ServerFilters.InitialiseRequestContext(contexts)
        .then(AddState(key))
        .then(PrintState(key))

    app(Request(GET, "/hello"))
}
