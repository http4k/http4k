package org.http4k.connect.amazon

import org.http4k.client.JavaHttpClient
import java.util.UUID

interface RealAwsContract : AwsContract {
    override val aws get() = configAwsEnvironment()
    override val http get() = JavaHttpClient()
    override fun uuid(seed: Int) = UUID.randomUUID()
}
