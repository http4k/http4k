package org.http4k.aws

import org.http4k.core.Request

// AWS fail to match the signature if it contains a '+' character in the path
// See https://jamesd3142.wordpress.com/2018/02/28/amazon-s3-and-the-plus-symbol/
internal fun Request.encodePlusCharInPath() = uri(uri.path(uri.path.replace("+", "%2B")))
