package org.http4k.connect.amazon.dynamodb

import org.http4k.connect.amazon.RealAwsContract
import java.time.Duration

class RealDynamoDbTest : DynamoDbContract, RealAwsContract {
    override val duration: Duration get() = Duration.ofSeconds(10)
}
