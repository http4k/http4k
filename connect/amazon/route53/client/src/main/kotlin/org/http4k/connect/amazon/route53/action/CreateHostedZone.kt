package org.http4k.connect.amazon.route53.action

import dev.forkhandles.result4k.Result4k
import org.http4k.connect.Action
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.core.firstChild
import org.http4k.connect.amazon.route53.model.ChangeInfo
import org.http4k.connect.amazon.route53.model.CreateHostedZoneResponse
import org.http4k.connect.amazon.route53.model.DelegationSet
import org.http4k.connect.amazon.route53.model.HostedZone
import org.http4k.connect.amazon.route53.model.HostedZoneConfig
import org.http4k.connect.amazon.route53.model.HostedZoneName
import org.http4k.connect.amazon.route53.model.VPC
import org.http4k.core.Method
import org.w3c.dom.Document

@Http4kConnectAction
class CreateHostedZone(
    val name: HostedZoneName,
    val callerReference: String,
    val delegationSetId: String?,
    val hostedZoneConfig: HostedZoneConfig?,
    val vpc: VPC?,
) : Route53Action<CreateHostedZoneResponse>(Method.POST, "/2013-04-01/hostedzone", ::parse),
    Action<Result4k<CreateHostedZoneResponse, RemoteFailure>>
{

    override fun toXml() = buildString {
        append("""<?xml version="1.0" encoding="UTF-8"?>""")
        append("""<CreateHostedZoneRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">""")
        append("<CallerReference>$callerReference</CallerReference>")
        if (delegationSetId != null) append("<DelegationSetId>$delegationSetId</DelegationSetId>")
        append("<Name>$name</Name>")
        if (hostedZoneConfig != null) append(hostedZoneConfig.toXml())
        if (vpc != null) append(vpc.toXml())
        append("</CreateHostedZoneRequest>")
    }
}

private fun parse(document: Document) = document.getElementsByTagName("CreateHostedZoneResponse")
    .item(0)
    .let { node ->
        CreateHostedZoneResponse(
            changeInfo = node.firstChild("ChangeInfo")!!.let(ChangeInfo::parse),
            delegationSet = node.firstChild("DelegationSet")?.let(DelegationSet::parse),
            hostedZone = HostedZone.parse(node.firstChild("HostedZone")!!),
            vpc = node.firstChild("VPC")?.let(VPC::parse)
        )
    }
