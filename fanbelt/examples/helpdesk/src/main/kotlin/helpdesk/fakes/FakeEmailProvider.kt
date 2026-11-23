package helpdesk.fakes

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.body.form
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

data class SentEmail(val to: String, val subject: String)

class FakeEmailProvider : HttpHandler {
    private val up = AtomicBoolean(true)

    private val outbox = CopyOnWriteArrayList<SentEmail>()

    val sent: List<SentEmail> get() = outbox.toList()

    val isUp get() = up.get()

    fun set(available: Boolean) = up.set(available)

    override fun invoke(request: Request) = when {
        up.get() -> Response(ACCEPTED).also { outbox += SentEmail(request.form("to").orEmpty(), request.form("subject").orEmpty()) }
        else -> Response(SERVICE_UNAVAILABLE)
    }
}