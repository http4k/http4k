package org.http4k.connect.amazon.route53.action

import org.http4k.connect.amazon.route53.model.GetHostedZoneResponse
import org.http4k.connect.amazon.route53.model.HostedZoneId
import org.http4k.core.Method

class GetHostedZone(
    val id: HostedZoneId
): Route53Action<GetHostedZoneResponse>(Method.GET, "/2013-04-01/hostedzone/$id", GetHostedZoneResponse::parse) {
    override fun toXml() = ""
}
