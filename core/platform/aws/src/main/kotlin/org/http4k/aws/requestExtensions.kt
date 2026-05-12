package org.http4k.aws

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.urlEncoded


internal fun Request.encodeUri() =
    uri(uri.encodePathAndFragment())

// AWS SigV4: S3 single-encodes the canonical path; every other service double-encodes.
// See https://docs.aws.amazon.com/IAM/latest/UserGuide/create-signed-request.html
internal fun Request.canonicalEncodeUri(scope: AwsCredentialScope) =
    if (scope.service == "s3") this else encodeUri()

private fun Uri.encodePathAndFragment() = if (fragment.isBlank())
    path(path.urlEncodedPath())
else
    path("${path}#${fragment}".urlEncodedPath()).fragment("")

private fun String.urlEncodedPath() =
    split("/").joinToString("/") { it.urlEncoded().replace("+", "%20").replace("*", "%2A").replace("%7E", "~") }

