package org.http4k.connect.amazon

import org.http4k.client.JavaHttpClient
import org.http4k.util.PortBasedTest
import java.util.UUID

interface RealAwsContract : AwsContract, PortBasedTest {
    override val aws get() = configAwsEnvironment()
    override val http get() = JavaHttpClient()
    override fun uuid(seed: Int) = UUID.randomUUID()
}
