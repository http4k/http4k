package org.http4k.wiretap

import io.opentelemetry.api.OpenTelemetry
import org.http4k.core.HttpHandler
import java.time.Clock

fun interface WiretapAppBuilder {
    operator fun invoke(http: HttpHandler, oTel: OpenTelemetry, clock: Clock): HttpHandler
}
