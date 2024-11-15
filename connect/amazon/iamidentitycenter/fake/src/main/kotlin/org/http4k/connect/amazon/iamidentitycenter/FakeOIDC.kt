package org.http4k.connect.amazon.iamidentitycenter

import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.iamidentitycenter.endpoints.createToken
import org.http4k.connect.amazon.iamidentitycenter.endpoints.deviceAuthorization
import org.http4k.connect.amazon.iamidentitycenter.endpoints.registerClient
import org.http4k.routing.routes
import java.time.Clock

class FakeOIDC(clock: Clock = Clock.systemUTC()) : ChaoticHttpHandler() {

    override val app = routes(
        registerClient(clock),
        deviceAuthorization(),
        createToken(),
    )
}

fun main() {
    FakeOIDC().start()
}
