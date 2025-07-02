package org.http4k.connect.amazon.route53.endpoints

import org.http4k.connect.amazon.core.firstChild
import org.http4k.connect.amazon.core.firstChildText
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.core.model.VpcId
import org.http4k.connect.amazon.core.xmlDoc
import org.http4k.connect.amazon.route53.action.CreateHostedZone
import org.http4k.connect.amazon.route53.model.ChangeInfo
import org.http4k.connect.amazon.route53.model.HostedZoneConfig
import org.http4k.connect.amazon.route53.model.HostedZoneId
import org.http4k.connect.amazon.route53.model.StoredHostedZone
import org.http4k.connect.amazon.route53.model.StoredResource
import org.http4k.connect.amazon.route53.model.VpcConfig
import org.http4k.connect.amazon.route53.model.save
import org.http4k.connect.amazon.route53.model.toHostedZone
import org.http4k.connect.amazon.route53.model.toXml
import org.http4k.connect.storage.Storage
import org.http4k.core.ContentType
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.contentType
import org.w3c.dom.Node
import java.time.Clock
import java.util.UUID

fun createHostedZone(
    hostedZones: Storage<StoredHostedZone>,
    resources: Storage<StoredResource>,
    vpcAssociations: Storage<VpcConfig>,
    clock: Clock
) = { request: Request ->
    val data = request.body.xmlDoc()
        .getElementsByTagName("CreateHostedZoneRequest")
        .item(0)
        .parse()

    val hostedZone = StoredHostedZone(
        id = HostedZoneId.parse(UUID.randomUUID().toString()),
        name = data.name,
        config = data.hostedZoneConfig,
        callerReference = data.callerReference
    )
    hostedZones[hostedZone.id.value] = hostedZone

    data.vpc?.let {
        vpcAssociations.save(hostedZone.id, it)
    }

    val change = ChangeInfo(
        id = UUID.randomUUID().toString(),
        status = ChangeInfo.Status.INSYNC,
        submittedAt = clock.instant(),
        comment = null
    )

    Response(Status.CREATED)
        .contentType(ContentType.APPLICATION_XML)
        .body("""<?xml version="1.0" encoding="UTF-8"?>
<CreateHostedZoneResponse>
   ${change.toXml()}
   ${hostedZone.toHostedZone(resources).toXml()}
   ${data.vpc?.toXml().orEmpty()}
</CreateHostedZoneResponse>""")
}


private fun Node.parse(): CreateHostedZone {
    return CreateHostedZone(
        name = firstChildText("Name")!!,
        callerReference = firstChildText("CallerReference")!!,
        hostedZoneConfig = firstChild("HostedZoneConfig")?.let {
            HostedZoneConfig(
                comment = it.firstChildText("Comment"),
                privateZone = it.firstChildText("PrivateZone")?.toBoolean()
            )
        },
        vpc = firstChild("VPC")?.let {
            VpcConfig(
                vpcId = VpcId.parse(it.firstChildText("VPCId")!!),
                vpcRegion = Region.parse(it.firstChildText("VPCRegion")!!)
            )
        }
    )
}
