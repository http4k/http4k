package org.http4k.connect.amazon.route53.endpoints

import dev.forkhandles.result4k.asSuccess
import org.http4k.connect.amazon.core.firstChild
import org.http4k.connect.amazon.core.firstChildText
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.core.model.VpcId
import org.http4k.connect.amazon.route53.action.CreateHostedZone
import org.http4k.connect.amazon.route53.model.ChangeInfo
import org.http4k.connect.amazon.route53.model.Config
import org.http4k.connect.amazon.route53.model.CreateHostedZoneResponse
import org.http4k.connect.amazon.route53.model.HostedZoneConfig
import org.http4k.connect.amazon.route53.model.HostedZoneId
import org.http4k.connect.amazon.route53.model.HostedZoneName
import org.http4k.connect.amazon.route53.model.ResourceRecordSet
import org.http4k.connect.amazon.route53.model.StoredHostedZone
import org.http4k.connect.amazon.route53.model.VPC
import org.http4k.connect.amazon.route53.model.invalidInput
import org.http4k.connect.amazon.route53.model.random
import org.http4k.connect.amazon.route53.model.save
import org.http4k.connect.amazon.route53.model.toHostedZone
import org.http4k.connect.amazon.route53.model.toXml
import org.http4k.connect.storage.Storage
import org.w3c.dom.Document
import java.time.Clock
import java.util.UUID
import kotlin.random.Random

fun createHostedZone(
    random: Random,
    hostedZones: Storage<StoredHostedZone>,
    resources: Storage<ResourceRecordSet>,
    vpcAssociations: Storage<VPC>,
    clock: Clock
) = route53FakeAction(::fromXml, ::toXml) fn@{ data ->
    if (data.hostedZoneConfig?.privateZone == true && data.vpc == null) {
        return@fn invalidInput("When you're creating a private hosted zone (when you specify true for PrivateZone), you must also specify values for VPCId and VPCRegion.")
    }

    val hostedZone = StoredHostedZone(
        id = HostedZoneId.random(random),
        name = data.name,
        config = data.hostedZoneConfig?.let {
            Config(
                comment = it.comment,
                privateZone = it.privateZone
            )
        },
        callerReference = data.callerReference
    )
    hostedZones[hostedZone.id.value] = hostedZone

    data.vpc?.let {
        vpcAssociations.save(hostedZone.id, it)
    }

    // create required resources
    resources["NS:${hostedZone.name}"] = ResourceRecordSet(
        name = hostedZone.name.value,
        type = ResourceRecordSet.Type.NS,
        aliasTarget = null,
        ttl = 172800,
        resourceRecords = listOf("ns-0000.fakedns.com")
    )
    resources["SOA:${hostedZone.name}"] = ResourceRecordSet(
        name = hostedZone.name.value,
        type = ResourceRecordSet.Type.SOA,
        aliasTarget = null,
        ttl = 900,
        resourceRecords = listOf("ns-0000.fakedns.com")
    )

    CreateHostedZoneResponse(
        changeInfo = ChangeInfo(
            id = UUID.randomUUID().toString(),
            status = ChangeInfo.Status.INSYNC,
            submittedAt = clock.instant(),
            comment = null
        ),
        hostedZone = hostedZone.toHostedZone(resources),
        vpc = data.vpc,
        delegationSet = null
    ).asSuccess()
}

private fun toXml(result: CreateHostedZoneResponse) = buildString {
    append("<CreateHostedZoneResponse>")
    append(result.changeInfo.toXml())
    append(result.hostedZone.toXml())
    result.vpc?.let { append(it.toXml()) }
    append("</CreateHostedZoneResponse>")
}

private fun fromXml(document: Document) = document
    .getElementsByTagName("CreateHostedZoneRequest")
    .item(0).let {
        CreateHostedZone(
            name = HostedZoneName.parse(it.firstChildText("Name")!!),
            callerReference = it.firstChildText("CallerReference")!!,
            delegationSetId = it.firstChild("DelegationSet")?.firstChildText("Id"),
            hostedZoneConfig = it.firstChild("HostedZoneConfig")?.let { config ->
                HostedZoneConfig(
                    comment = config.firstChildText("Comment"),
                    privateZone = config.firstChildText("PrivateZone")?.toBoolean()
                )
            },
            vpc = it.firstChild("VPC")?.let { vpc ->
                VPC(
                    vpcId = VpcId.parse(vpc.firstChildText("VPCId")!!),
                    vpcRegion = Region.parse(vpc.firstChildText("VPCRegion")!!)
                )
            }
        )
    }
