package cookbook.request_context

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.ServerFilters

fun main(args: Array<String>) {
    data class SharedState(val message: String)

    fun AddState(contexts: RequestContexts) = Filter { next ->
        {
            contexts[it].set("myKey", SharedState("hello there"))
            next(it)
        }
    }

    fun PrintState(contexts: RequestContexts): HttpHandler = { request ->
        println(contexts[request].get<SharedState>("myKey"))
        Response(Status.OK)
    }

    // this is the shared RequestContexts object - it holds the bag of state for each request and
    // tidies up afterwards
    val contexts = RequestContexts()

    // The first Filter is required to initialise the bag of state.
    // The second Filter modifies the bag
    // The handler just prints out the state
    val app = ServerFilters.InitialiseRequestContext(contexts)
        .then(AddState(contexts))
        .then(PrintState(contexts))

    app(Request(Method.GET, "/hello"))
}
