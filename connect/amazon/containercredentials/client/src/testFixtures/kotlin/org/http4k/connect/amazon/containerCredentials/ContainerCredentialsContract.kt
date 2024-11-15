package org.http4k.connect.amazon.containerCredentials

import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.containercredentials.ContainerCredentials
import org.http4k.connect.amazon.containercredentials.Http
import org.http4k.connect.amazon.containercredentials.action.getCredentials
import org.http4k.connect.successValue
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.ZonedDateTime

interface ContainerCredentialsContract : AwsContract {
    val clock get() = Clock.systemUTC()!!
    private val containerCredentials get() = ContainerCredentials.Http(http)
    val fullUri: Uri

    @Test
    fun `get credentials`() {
        val result = containerCredentials.getCredentials(fullUri)

        assertTrue(
            result.successValue()
                .Expiration.value.isAfter(ZonedDateTime.now(clock))
        )
    }
}
