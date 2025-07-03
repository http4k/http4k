package org.http4k.connect.amazon.route53.endpoints

import dev.forkhandles.result4k.asSuccess
import org.http4k.connect.amazon.route53.hostedZoneIdLens
import org.http4k.connect.amazon.route53.model.ChangeInfo
import org.http4k.connect.amazon.route53.model.HostedZoneName
import org.http4k.connect.amazon.route53.model.ResourceRecordSet
import org.http4k.connect.amazon.route53.model.StoredHostedZone
import org.http4k.connect.amazon.route53.model.forDomain
import org.http4k.connect.amazon.route53.model.hostedZoneNotEmpty
import org.http4k.connect.amazon.route53.model.noSuchHostedZone
import org.http4k.connect.amazon.route53.model.toXml
import org.http4k.connect.storage.Storage
import java.time.Clock
import java.util.UUID

fun deleteHostedZone(
    hostedZones: Storage<StoredHostedZone>,
    resourceRecords: Storage<ResourceRecordSet>,
    clock: Clock
) = route53FakeAction(
    responseBodyFn = { "<DeleteHostedZoneResponse>${it.toXml()}</DeleteHostedZoneResponse>" }
) fn@{
    val hostedZoneId = hostedZoneIdLens(this)
    val hostedZone = hostedZones[hostedZoneId.value] ?: return@fn noSuchHostedZone(hostedZoneId)
    if (resourceRecords.forDomain(hostedZone.name).any { it.isDeletable(hostedZone.name) }) {
        return@fn hostedZoneNotEmpty()
    }

    hostedZones -= hostedZone.id.value

    ChangeInfo(
        id = UUID.randomUUID().toString(),
        status = ChangeInfo.Status.INSYNC,
        submittedAt = clock.instant(),
        comment = null
    ).asSuccess()
}

private fun ResourceRecordSet.isDeletable(hostedZoneName: HostedZoneName) = when (type) {
    ResourceRecordSet.Type.NS, ResourceRecordSet.Type.SOA -> name != hostedZoneName.value
    else -> true
}
