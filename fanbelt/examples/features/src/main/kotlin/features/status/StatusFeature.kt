package features.status

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.fanbelt.feature.Feature
import java.time.Clock
import java.time.Duration.ofMinutes

data class ServiceStatus(val message: String) {
    companion object {
        val UNKNOWN = ServiceStatus("Unknown")
    }
}

class StatusFeature(private val http: HttpHandler, private val clock: Clock) : Feature<Unit, Unit>({ }) {
    val current by derived { ctx, _: Unit ->
        ctx.stale(at = clock.instant() + ofMinutes(1))
        val response = http(Request(GET, "/status/${ctx.tenant}"))
        if (response.status.successful) ServiceStatus(response.bodyString()) else ServiceStatus.UNKNOWN
    }
}
