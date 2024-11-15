package org.http4k.connect.amazon.sqs

import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.RealAwsContract
import java.time.Duration

class RealSQSTest : SQSContract, RealAwsContract {
    override val http = JavaHttpClient()


    override val retryTimeout: Duration = Duration.ofMinutes(1)
    override fun waitABit() {
        Thread.sleep(10000)
    }
}
