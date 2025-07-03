package org.http4k.connect.amazon.route53.action

import dev.forkhandles.result4k.Result4k
import org.http4k.connect.Action
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.route53.model.GetHostedZoneResponse
import org.http4k.connect.amazon.route53.model.HostedZoneId
import org.http4k.core.Method

@Http4kConnectAction
class GetHostedZone(
    val id: HostedZoneId
) : Route53Action<GetHostedZoneResponse>(Method.GET, "/2013-04-01/hostedzone/$id", GetHostedZoneResponse::parse),
    Action<Result4k<GetHostedZoneResponse, RemoteFailure>>
{
    override fun toXml() = ""
}
