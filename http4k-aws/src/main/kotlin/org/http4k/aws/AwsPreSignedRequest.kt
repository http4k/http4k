package org.http4k.aws

import org.http4k.core.Headers
import org.http4k.core.Uri
import java.time.Instant

data class AwsPreSignedRequest(
    val uri: Uri,
    val signedHeaders: Headers,
    val expires: Instant
)
