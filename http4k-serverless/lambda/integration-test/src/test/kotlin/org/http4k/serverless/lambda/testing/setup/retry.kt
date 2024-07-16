package org.http4k.serverless.lambda.testing.setup

import org.http4k.config.Timeout
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.fail
import java.time.Duration
import java.time.Instant

fun retryUntil(
    status: Status,
    timeout: Timeout = Timeout(Duration.ofSeconds(5)),
    retryEvery: Duration = Duration.ofMillis(500),
    action: () -> Response
): Response {
    val start = Instant.now()
    var response: Response
    var success: Boolean
    do {
        response = action()
        success = response.status == status
        if (!success) {
            if (Duration.ofMillis(Instant.now().toEpochMilli() - start.toEpochMilli()) > timeout.value) {
                fail("Timed out after ${timeout.value}. Last response is ${response.status}:\n${response.bodyString()}")
            }
            Thread.sleep(retryEvery.toMillis())
        }
    } while (!success)
    return response
}
