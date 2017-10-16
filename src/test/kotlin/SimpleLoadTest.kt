import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.server.Jetty
import org.http4k.server.Netty
import org.http4k.server.ServerConfig
import org.http4k.server.SunHttp
import org.http4k.server.Undertow
import org.http4k.server.asServer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

data class Result(val name: String, val time: Long, val totalRequests: Int, val errors: Long) {
    private val errorPct = errors.toDouble() / totalRequests
    private val latency = time / totalRequests
    override fun toString(): String = """$name $totalRequests requests took ${time}ms with $errors errors ($errorPct), ${latency}ms}"""
}

fun testWith(threads: Int, reps: Int, fn: (Int) -> ServerConfig, port: Int): Result {
    val config = fn(port)
    val server = { _: Request -> Response(Status.OK) }.asServer(config).start()
    Thread.sleep(1000)

    val client = ApacheClient()
    val latch = CountDownLatch(threads)
    val start = System.currentTimeMillis()
    val errors = AtomicLong(0)
    (0..threads).forEach({
        thread {
            (0..reps).map {
                if (client(Request(Method.GET, "http://localhost:$port")).status != OK) {
                    errors.incrementAndGet()
                }
            }
            latch.countDown()
        }
    })
    latch.await()
    server.stop()
    return Result(config.javaClass.simpleName, System.currentTimeMillis() - start, threads * reps, errors.get())
}

fun main(args: Array<String>) {
    val threads = 200
    val reps = 100

    listOf(::SunHttp, ::Jetty, ::Netty, ::Undertow)
        .map { testWith(threads, reps, it, 8000) }
        .sortedBy { it.time }
        .forEach(::println)
}