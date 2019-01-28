package guide.modules.jsonrpc

import guide.modules.message_formats.json
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.format.Json
import org.http4k.jsonrpc.ErrorHandler
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpc
import java.util.concurrent.atomic.AtomicInteger

class Counter {
    private val value = AtomicInteger()

    fun increment(amount: Increment): Int = when {
        amount.value == 10 -> throw RuntimeException("Boom!")
        amount.value < 0 -> throw NegativeIncrementException()
        else -> value.addAndGet(amount.value)
    }

    fun currentValue(): Int = value.get()

    data class Increment(val value: Int)

    class NegativeIncrementException : RuntimeException("negative increment not allowed")
}

object CounterErrorHandler : ErrorHandler {
    override fun invoke(error: Throwable): ErrorMessage? = when (error) {
        is Counter.NegativeIncrementException -> NegativeIncrementExceptionMessage()
        else -> null
    }

    private class NegativeIncrementExceptionMessage : ErrorMessage(1, "Increment by negative") {
        override fun <NODE> data(json: Json<NODE>) = json.string("cannot increment counter by negative")
    }
}

fun main() {
    val counter = Counter()

    val rpcHandler: HttpHandler = JsonRpc.auto(json, CounterErrorHandler) {
        method("increment", handler(counter::increment))
        method("incrementDefinedFields", handler(setOf("value", "ignored"), counter::increment))
        method("current", handler(counter::currentValue))
    }

    val request = """ {"jsonrpc": "2.0", "method": "increment", "params": {"value": -1}, "id": 1} """

    println(
        rpcHandler(
            Request(Method.POST, "/rpc")
                .header("Content-Type", "application/json")
                .body(request)
        )
    )
}