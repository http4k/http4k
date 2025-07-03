package org.http4k.connect.amazon.route53

import org.http4k.connect.amazon.RealAwsContract
import org.junit.jupiter.api.BeforeEach
import java.time.Duration

class RealRoute53Test: Route53Contract, RealAwsContract {
    @BeforeEach
    fun mitigateRateLimiting() {
        Thread.sleep(Duration.ofSeconds(1))
    }
}
