package org.http4k.connect.amazon.iotdataplane

import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.RealAwsContract
import org.http4k.core.Uri
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.lang.System.getenv

/**
 * Unlike other AWS services the IoT data endpoint is account-specific, so it has to be supplied.
 * Set HTTP4K_IOT_DATA_PLANE_ENDPOINT to run this against a real account.
 */
class RealIotDataPlaneTest : IotDataPlaneContract, RealAwsContract {
    override val http = JavaHttpClient()

    override val endpoint: Uri
        get() = getenv("HTTP4K_IOT_DATA_PLANE_ENDPOINT")
            .also { assumeTrue(it != null, "HTTP4K_IOT_DATA_PLANE_ENDPOINT not set") }
            .let(Uri::of)
}
