package org.http4k.connect.amazon.route53.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.children
import org.http4k.connect.amazon.core.firstChild
import org.http4k.connect.amazon.core.firstChildText
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.core.model.VpcId
import org.http4k.connect.amazon.route53.model.ChangeInfo
import org.http4k.connect.amazon.route53.model.CreateHostedZoneResponse
import org.http4k.connect.amazon.route53.model.DelegationSet
import org.http4k.connect.amazon.route53.model.HostedZone
import org.http4k.connect.amazon.route53.model.HostedZoneConfig
import org.http4k.connect.amazon.route53.model.VpcConfig
import org.http4k.core.Method
import org.w3c.dom.Document
import java.util.UUID

@Http4kConnectAction
class CreateHostedZone(
    val name: String,
    val delegationSetId: String?,
    val hostedZoneConfig: HostedZoneConfig?,
    val vpc: VpcConfig?,
    val callerReference: String = UUID.randomUUID().toString(),
): Route53Action<CreateHostedZoneResponse>(Method.POST, "/2013-04-01/hostedzone", ::parse) {

    override fun toXml() = buildString {
        append("""<?xml version="1.0" encoding="UTF-8"?>""")
        append("""<CreateHostedZoneRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">""")
        append("<CallerReference>$callerReference</CallerReference>")
        append("<DelegationSetId>$delegationSetId</DelegationSetId>")
        append("<Name>$name</Name>")
        if (hostedZoneConfig != null) {
            append("<HostedZoneConfig><Comment>string</Comment><PrivateZone>boolean</PrivateZone></HostedZoneConfig>")
        }
        if (vpc != null) {
            append("<VPC><VPCId>${vpc.vpcId}</VPCId><VPCRegion>${vpc.vpcRegion}</VPCRegion></VPC>")
        }
        append("</CreateHostedZoneRequest>")
    }
}

private fun parse(document: Document) = CreateHostedZoneResponse(
    changeInfo = document.firstChild("ChangeId")!!.let(ChangeInfo::parse),
    delegationSet = document.firstChild("DelegationSet")!!.let { set ->
        DelegationSet(
            nameServers = document.children("NameServers")
                .map { it.firstChildText("NameServer")!! }
                .toList()
            ,
            callerReference = set.firstChildText("CallerReference")!!,
            id = set.firstChildText("Id")!!
        )
    },
    hostedZone = document.firstChild("HostedZone")?.let {
        HostedZone(
            name = it.firstChildText("Name")!!,
            id = it.firstChildText("Id")!!,
            callerReference = it.firstChildText("CallerReference")!!,
            config = it.firstChild("Config")?.let { config ->
                HostedZoneConfig(
                    comment = config.firstChildText("Comment"),
                    privateZone = config.firstChildText("PrivateZone")?.toBoolean()
                )
            },
            resourceRecordSetCount = it.firstChildText("ResourceRecordSetCount")?.toLong()
        )
    },
    vpc = document.firstChild("Vpc")?.let {
        VpcConfig(
            vpcId = VpcId.parse(it.firstChildText("VPCId")!!),
            vpcRegion = Region.parse(it.firstChildText("VPCRegion")!!)
        )
    }
)
