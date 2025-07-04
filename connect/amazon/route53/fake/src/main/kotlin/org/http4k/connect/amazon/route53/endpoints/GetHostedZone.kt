package org.http4k.connect.amazon.route53.endpoints

import dev.forkhandles.result4k.asSuccess
import org.http4k.connect.amazon.route53.hostedZoneIdLens
import org.http4k.connect.amazon.route53.model.GetHostedZoneResponse
import org.http4k.connect.amazon.route53.model.ResourceRecordSet
import org.http4k.connect.amazon.route53.model.StoredHostedZone
import org.http4k.connect.amazon.route53.model.VPC
import org.http4k.connect.amazon.route53.model.forZone
import org.http4k.connect.amazon.route53.model.noSuchHostedZone
import org.http4k.connect.amazon.route53.model.toHostedZone
import org.http4k.connect.amazon.route53.model.toXml
import org.http4k.connect.storage.Storage

fun getHostedZone(
    hostedZones: Storage<StoredHostedZone>,
    resources: Storage<ResourceRecordSet>,
    vpcs: Storage<VPC>
) = route53FakeAction(::toXml) fn@{
    val hostedZoneId = hostedZoneIdLens(this)
    val hostedZone = hostedZones[hostedZoneId.value] ?: return@fn noSuchHostedZone(hostedZoneId)

    GetHostedZoneResponse(
        hostedZone = hostedZone.toHostedZone(resources),
        vpcs = vpcs.forZone(hostedZone.id),
        delegationSet = null
    ).asSuccess()
}

private fun toXml(result: GetHostedZoneResponse) = buildString {
    append("""<GetHostedZoneResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">""")
    append(result.hostedZone.toXml())
    if (result.vpcs.isNotEmpty()) {
        append("<VPCs>")
        for (vpc in result.vpcs) append(vpc.toXml())
        append("</VPCs>")
    }
    append("</GetHostedZoneResponse>")
}
