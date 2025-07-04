package org.http4k.connect.amazon.route53.action

import dev.forkhandles.result4k.Result4k
import org.http4k.connect.Action
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.route53.model.ChangeInfo
import org.http4k.connect.amazon.route53.model.HostedZoneId
import org.http4k.core.Method
import org.w3c.dom.Document

@Http4kConnectAction
class DeleteHostedZone(
    val id: HostedZoneId
) : Route53Action<ChangeInfo>(Method.DELETE, "/2013-04-01/hostedzone/$id", ::parse),
    Action<Result4k<ChangeInfo, RemoteFailure>>
{
    override fun toXml() = ""
}

private fun parse(document: Document) = document
    .getElementsByTagName("ChangeInfo")
    .item(0)
    .let(ChangeInfo::parse)
