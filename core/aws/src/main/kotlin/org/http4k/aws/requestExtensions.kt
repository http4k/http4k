package org.http4k.aws

import org.http4k.core.Request

internal fun Request.encodePlusCharInPath() = uri(uri.path(uri.path.replace("+", "%2B")))
