package org.http4k.connect.amazon.iot

import org.http4k.connect.amazon.RealAwsContract
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.iot.model.S3Location
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import java.lang.System.getenv

/**
 * The contract creates jobs targeted at a real thing and streams read by a real role, so both
 * have to be supplied. Set HTTP4K_IOT_THING_ARN to the ARN of an existing thing and
 * HTTP4K_IOT_STREAM_ROLE_ARN to a role IoT can assume, to run this against a real account.
 */
class RealIotTest : IotContract, RealAwsContract {
    override val thingArn: ARN
        get() = getenv("HTTP4K_IOT_THING_ARN")
            .also { assumeTrue(it != null, "HTTP4K_IOT_THING_ARN not set") }
            .let(ARN::of)

    override val streamRoleArn: ARN
        get() = getenv("HTTP4K_IOT_STREAM_ROLE_ARN")
            .also { assumeTrue(it != null, "HTTP4K_IOT_STREAM_ROLE_ARN not set") }
            .let(ARN::of)

    override val streamS3Location: S3Location
        get() = S3Location(
            bucket = getenv("HTTP4K_IOT_STREAM_S3_BUCKET")
                .also { assumeTrue(it != null, "HTTP4K_IOT_STREAM_S3_BUCKET not set") },
            key = getenv("HTTP4K_IOT_STREAM_S3_KEY")
                .also { assumeTrue(it != null, "HTTP4K_IOT_STREAM_S3_KEY not set") },
        )

    /** Every case is gated, including the ones which never read the configuration themselves. */
    @BeforeEach
    fun assumeRealAccountConfigured() {
        thingArn
        streamRoleArn
        streamS3Location
    }
}
