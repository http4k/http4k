package org.http4k.connect.amazon.route53.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.sequenceOfNodes
import org.http4k.connect.amazon.route53.model.Change
import org.http4k.connect.amazon.route53.model.ChangeInfo
import org.http4k.connect.amazon.route53.model.HostedZoneId
import org.http4k.core.Method
import org.w3c.dom.Document

@Http4kConnectAction
class ChangeResourceRecordSets(
    val hostedZoneId: HostedZoneId,
    val changes: List<Change>
) : Route53Action<ChangeInfo>(Method.POST, "/2013-04-01/hostedzone/$hostedZoneId/rrset", ::parse) {

    override fun toXml() = buildString {
        append("""<?xml version="1.0" encoding="UTF-8"?>""")
        append("""<ChangeResourceRecordSetsRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">""")
        append("<ChangeBatch>")
        changes.forEach { append(it.toXml()) }
        append("</ChangeBatch>")
        append("</ChangeResourceRecordSetsRequest>")
    }
}

private fun parse(document: Document) = document
    .getElementsByTagName("ChangeResourceRecordSetsResponse")
    .sequenceOfNodes("ChangeInfo")
    .first()
    .let(ChangeInfo::parse)
