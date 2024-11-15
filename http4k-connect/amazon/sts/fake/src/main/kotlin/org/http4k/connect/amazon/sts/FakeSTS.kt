package org.http4k.connect.amazon.sts

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.Method.POST
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Clock
import java.time.Duration
import java.time.Duration.ofHours

class FakeSTS(
    private val clock: Clock = Clock.systemUTC(),
    defaultSessionValidity: Duration = ofHours(1)
) : ChaoticHttpHandler() {

    override val app = routes(
        "/" bind POST to routes(
            assumeRole(defaultSessionValidity, clock),
            assumeRoleWithWebIdentity(defaultSessionValidity, clock)
        )
    )

    /**
     * Convenience function to get a STS client
     */
    fun client() = STS.Http(
        Region.of("ldn-north-1"),
        { AwsCredentials("accessKey", "secret") }, this, clock
    )
}

fun main() {
    FakeSTS().start()
}
