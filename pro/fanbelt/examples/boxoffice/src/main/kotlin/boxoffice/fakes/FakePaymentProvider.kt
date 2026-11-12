package boxoffice.fakes

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.PAYMENT_REQUIRED
import java.util.concurrent.atomic.AtomicBoolean

class FakePaymentProvider : HttpHandler {
    private val declining = AtomicBoolean(false)

    fun declines() = declining.set(true)

    override fun invoke(request: Request) = Response(if (declining.get()) PAYMENT_REQUIRED else OK)
}