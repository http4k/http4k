package org.http4k.aws

import org.http4k.core.Request
import org.http4k.core.Uri


internal fun Request.encodeUriToMatchSignature() =
    uri(uri.encodeFragmentInPath().encodePlusCharInPath())

// AWS fail to match the signature if it contains a '+' character in the path
// See https://jamesd3142.wordpress.com/2018/02/28/amazon-s3-and-the-plus-symbol/
private fun Uri.encodePlusCharInPath() = path(path.replace("+", "%2B").replace(" ", "+"))

private fun Uri.encodeFragmentInPath() =
    if (fragment.isBlank()) this else path("$path#${fragment}".replace("#", "%23")).fragment("")
