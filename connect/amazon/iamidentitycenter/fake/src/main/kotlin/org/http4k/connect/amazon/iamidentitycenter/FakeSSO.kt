package org.http4k.connect.amazon.iamidentitycenter

import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.iamidentitycenter.endpoints.getFederatedCredentials
import org.http4k.routing.routes
import java.time.Clock

class FakeSSO(clock: Clock = Clock.systemUTC()) : ChaoticHttpHandler() {
    override val app = routes(
        getFederatedCredentials(clock)
    )
}

fun main() {
    FakeSSO().start()
}
