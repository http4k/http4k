package org.http4k.connect.amazon.containercredentials

import org.http4k.base64Encode
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AccessKeyId
import org.http4k.connect.amazon.core.model.Credentials
import org.http4k.connect.amazon.core.model.Expiration
import org.http4k.connect.amazon.core.model.SecretAccessKey
import org.http4k.connect.amazon.core.model.SessionToken
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import java.time.Clock
import java.time.Duration
import java.time.Duration.ofHours
import java.time.ZonedDateTime
import java.util.UUID

class FakeContainerCredentials(
    private val clock: Clock = Clock.systemUTC(),
    defaultSessionValidity: Duration = ofHours(1)
) : ChaoticHttpHandler() {

    override val app = { _: Request ->
        Response(OK).body(
            ContainerCredentialsMoshi.asFormatString(
                Credentials(
                    SessionToken.of(UUID.randomUUID().toString().base64Encode()),
                    AccessKeyId.of("accessKeyId"),
                    SecretAccessKey.of("secretAccessKey"),
                    Expiration.of(ZonedDateTime.now(clock) + defaultSessionValidity),
                    ARN.of("arn:aws:iam::111111111111:role/test-service")
                )
            )
        )
    }

    /**
     * Convenience function to get a ContainerCredentials client
     */
    fun client() = ContainerCredentials.Http(this)
}

fun main() {
    FakeContainerCredentials().start()
}
