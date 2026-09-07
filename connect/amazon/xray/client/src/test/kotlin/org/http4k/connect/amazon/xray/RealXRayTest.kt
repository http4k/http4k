package org.http4k.connect.amazon.xray

import org.http4k.connect.amazon.RealAwsContract
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import java.lang.System.getenv

/**
 * Set HTTP4K_XRAY_ENABLED to run against a real account. The contract reads traces and writes none.
 */
class RealXRayTest : XRayContract, RealAwsContract {
    @BeforeEach
    fun assumeRealAccountConfigured() {
        assumeTrue(getenv("HTTP4K_XRAY_ENABLED") != null, "HTTP4K_XRAY_ENABLED not set")
    }
}
