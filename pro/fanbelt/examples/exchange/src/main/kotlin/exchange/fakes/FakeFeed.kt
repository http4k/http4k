package exchange.fakes

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import java.time.Clock
import java.util.Random

class FakeFeed(private val clock: Clock, private val random: Random = Random()) : HttpHandler {
    private val prices = mutableMapOf("ORN" to 100.0, "HTK" to 85.0, "DSTR" to 140.0, "KTLN" to 60.0, "JVM" to 210.0)
    private var second = 0L

    @Synchronized
    override fun invoke(request: Request): Response {
        val now = clock.instant().epochSecond
        (second until now).take(5).forEach { _ -> prices.replaceAll { _, price -> (price * (1 + random.nextGaussian() * 0.004 + if (random.nextInt(40) == 0) random.nextGaussian() * 0.03 else 0.0)).coerceAtLeast(1.0) } }
        second = now
        return Response(OK).body(prices.entries.joinToString("\n") { (symbol, price) -> "$symbol %.2f".format(price) })
    }
}