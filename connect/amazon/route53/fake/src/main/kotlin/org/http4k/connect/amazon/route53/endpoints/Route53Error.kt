package org.http4k.connect.amazon.route53.endpoints

import org.http4k.core.Status

data class Route53Error(
    val status: Status,
    val message: String
)

fun noSuchHostedZone() = Route53Error(Status.NOT_FOUND, "NoSuchHostedZone")
fun invalidChangeBatch() = Route53Error(Status.BAD_REQUEST, "InvalidChangeBatch")
