package exchange

import exchange.domain.SYMBOLS
import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode
import org.http4k.core.ContentEncoding
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import java.io.FilterInputStream
import java.io.InputStream
import java.net.http.HttpClient
import java.time.Duration
import java.util.Random
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import org.http4k.core.Brotli

private val BOTS = System.getenv("BOTS")?.toInt() ?: 30
private val NODES = System.getenv("NODES")?.toInt() ?: 3
private val THINK = System.getenv("THINK_MS")?.toLong() ?: 500
private val ACCEPT_ENCODING = System.getenv("ACCEPT_ENCODING")?.takeIf(String::isNotBlank) ?: "br"

private val actions = AtomicLong()
private val failures = AtomicLong()
private val posts = AtomicLong()
private val latencies = ConcurrentLinkedQueue<Long>()
private val wire = AtomicLong()
private val decoded = AtomicLong()
private val brotli = ContentEncoding.Brotli()
private val http = JavaHttpClient(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(), BodyMode.Memory, BodyMode.Stream) {
    it.timeout(Duration.ofSeconds(5))
}

fun main(@Suppress("UNUSED_PARAMETER") args: Array<String>) {
    (0 until BOTS).forEach { id -> Thread.ofVirtual().name("bot-$id").start { HttpBot(id).run() } }
    var last = listOf(0L, 0L, 0L, 0L, 0L)
    while (true) {
        Thread.sleep(10_000)
        val now = listOf(actions.get(), posts.get(), failures.get(), wire.get(), decoded.get())
        val (acted, posted, failed, onTheWire, content) = now.zip(last) { a, b -> a - b }
        last = now
        val period = generateSequence { latencies.poll() }.toList().sorted()
        println("%d http bots (%s), last 10s: %.1f actions/s, %.1f orders and cancels/s, %d failures, post latency p50 %s p95 %s max %s, %d KB/s on the wire for %d KB/s of content"
            .format(BOTS, ACCEPT_ENCODING, acted / 10.0, posted / 10.0, failed, period.at(0.5), period.at(0.95), period.at(1.0), onTheWire / 10240, content / 10240))
    }
}

private fun List<Long>.at(p: Double) = if (isEmpty()) "n/a" else "%.0fms".format(this[((size - 1) * p).toInt()] / 1e6)

private class HttpBot(val id: Int) {
    private val random = Random(id.toLong())
    private val base = "http://localhost:${9000 + id % NODES}/venues/demo/"
    private val strategy = Strategy.entries[id % Strategy.entries.size]
    private var symbol = SYMBOLS[random.nextInt(SYMBOLS.size)]
    private var nextHop = hop()
    private lateinit var viewer: Cookie
    private var stream: InputStream? = null

    @Volatile private var mark: Double? = null
    @Volatile private var lastPrint: Double? = null
    @Volatile private var orders: List<String> = emptyList()

    private fun hop() = System.currentTimeMillis() + 20_000 + random.nextInt(20_000)

    fun run() {
        val page = http(Request(GET, base).header("accept-encoding", ACCEPT_ENCODING))
        viewer = checkNotNull(page.cookies().find { it.name == "viewer" }) { "bot $id was given no viewer cookie" }
        page.text()
        watch()
        while (true) {
            runCatching { act() }.onSuccess { actions.incrementAndGet() }.onFailure { failures.incrementAndGet(); System.err.println("bot $id: ${it.message?.lines()?.first()}") }
            Thread.sleep(THINK / 2 + random.nextLong(THINK))
        }
    }

    private fun watch() {
        stream?.close()
        mark = null
        lastPrint = null
        val watching = symbol
        val response = http(Request(GET, "${base}s/$watching").cookie(viewer).header("accept", "text/event-stream")
            .header("datastar-request", "true").header("accept-encoding", ACCEPT_ENCODING))
        stream = response.body.stream
        val events = response.decoded()
        Thread.ofPlatform().daemon().name("bot-$id-$watching").start {
            runCatching { events.bufferedReader().useLines { lines -> readEvents(lines, watching) } }
        }
    }

    private fun readEvents(lines: Sequence<String>, watching: String) {
        val event = StringBuilder()
        lines.forEach { line ->
            if (line.isEmpty()) {
                seen(event.toString(), watching)
                event.clear()
            } else if (line.startsWith("data:")) event.append(line.removePrefix("data:").removePrefix(" ")).append('\n')
        }
    }

    private fun seen(event: String, watching: String) {
        if (event.contains("id=\"ticker\"")) Regex("""id="px-$watching-\d+" class="flash[^"]*">([\d,.]+)""").find(event)?.let { mark = it.groupValues[1].number() }
        if (event.contains("id=\"tape\"")) TAPE_PRINT.find(event, event.indexOf("id=\"tape\""))?.let { lastPrint = it.groupValues[1].number() }
        if (event.contains("id=\"portfolio\"")) orders = CANCEL.findAll(event).map { it.groupValues[1] }.toList()
    }

    fun act() {
        if (System.currentTimeMillis() > nextHop) {
            symbol = (SYMBOLS - symbol)[random.nextInt(SYMBOLS.size - 1)]
            watch()
            nextHop = hop()
        }
        val mark = mark ?: return
        when (strategy) {
            Strategy.MarketMaker -> {
                if (orders.size > 6) post("orders/${orders.last()}/cancel", """{"price":"","quantity":""}""")
                val spread = mark * (0.001 + random.nextDouble() * 0.004)
                order("buy", "%.2f".format(mark - spread), 5 + random.nextInt(15))
                order("sell", "%.2f".format(mark + spread), 5 + random.nextInt(15))
            }
            Strategy.Noise -> order(if (random.nextBoolean()) "buy" else "sell", "", 1 + random.nextInt(10))
            Strategy.Momentum -> {
                val last = lastPrint ?: return
                if (random.nextInt(3) == 0) order(if (last > mark) "buy" else "sell", "", 1 + random.nextInt(8))
            }
        }
    }

    private fun order(side: String, price: String, quantity: Int) = post("s/$symbol/orders/$side", """{"price":"$price","quantity":"$quantity"}""")

    private fun post(path: String, signals: String) {
        val started = System.nanoTime()
        val response = http(Request(POST, base + path).cookie(viewer).header("datastar-request", "true")
            .header("content-type", "application/json").header("accept-encoding", ACCEPT_ENCODING).body(signals))
        response.text()
        latencies += System.nanoTime() - started
        posts.incrementAndGet()
        check(response.status.successful) { "$path answered ${response.status}" }
    }
}

private val TAPE_PRINT = Regex("""<td class="(?:up|down)">([\d,.]+)</td>""")
private val CANCEL = Regex("""@post\('orders/([^/']+)/cancel'\)""")

private fun String.number() = replace(",", "").toDouble()

private fun Response.decoded(): InputStream {
    val onTheWire = Counting(body.stream, wire)
    return Counting(if (header("content-encoding") == "br") brotli.decoder(onTheWire) else onTheWire, decoded)
}

private fun Response.text() = decoded().use { String(it.readAllBytes()) }

private class Counting(input: InputStream, private val count: AtomicLong) : FilterInputStream(input) {
    override fun read() = super.read().also { if (it >= 0) count.incrementAndGet() }

    override fun read(b: ByteArray, off: Int, len: Int) = super.read(b, off, len).also { if (it > 0) count.addAndGet(it.toLong()) }
}