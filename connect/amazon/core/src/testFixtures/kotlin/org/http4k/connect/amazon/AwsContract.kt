package org.http4k.connect.amazon

import org.http4k.core.HttpHandler
import java.util.UUID

interface AwsContract {
    val aws: AwsEnvironment
    val http: HttpHandler
    fun uuid(seed: Int = 0): UUID
}
