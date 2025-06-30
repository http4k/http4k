package org.http4k.connect.amazon.route53.action

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.sequenceOfNodes
import org.http4k.connect.amazon.core.xmlDoc
import org.http4k.connect.amazon.route53.model.Change
import org.http4k.connect.amazon.route53.model.ChangeInfo
import org.http4k.connect.amazon.route53.model.HostedZoneId
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response

@Http4kConnectAction
class ChangeResourceRecordSets(
    val hostedZoneId: HostedZoneId,
    val changes: List<Change>
) : Route53Action<ChangeInfo> {

    override fun toRequest(): Request {
        val xml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8"?>""")
            append("""<ChangeResourceRecordSetsRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">""")
            append("<ChangeBatch>")
            changes.forEach { append(it.toXml()) }
            append("</ChangeBatch>")
            append("</ChangeResourceRecordSetsRequest>")
        }

        return Request(Method.POST, "/2013-04-01/hostedzone/$hostedZoneId/rrset").body(xml)
    }

    override fun toResult(response: Response) = when {
        response.status.successful -> response.xmlDoc()
            .getElementsByTagName("ChangeResourceRecordSetsResponse")
            .sequenceOfNodes("ChangeInfo")
            .first()
            .let(ChangeInfo::parse)
            .asSuccess()
        else -> asRemoteFailure(response).asFailure()
    }
}
