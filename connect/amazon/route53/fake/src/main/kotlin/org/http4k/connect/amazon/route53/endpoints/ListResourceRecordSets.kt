package org.http4k.connect.amazon.route53.endpoints

import dev.forkhandles.result4k.asSuccess
import org.http4k.connect.amazon.route53.hostedZoneIdLens
import org.http4k.connect.amazon.route53.model.ListResourceRecordSetsResponse
import org.http4k.connect.amazon.route53.model.ResourceRecordSet
import org.http4k.connect.amazon.route53.model.StoredHostedZone
import org.http4k.connect.amazon.route53.model.forDomain
import org.http4k.connect.amazon.route53.model.noSuchHostedZone
import org.http4k.connect.storage.Storage

fun listResourceRecordSets(
    hostedZones: Storage<StoredHostedZone>,
    resources: Storage<ResourceRecordSet>
) = route53FakeAction(::serializeResponse) fn@{
    val hostedZoneId = hostedZoneIdLens(this)
    val hostedZone = hostedZones[hostedZoneId.value] ?: return@fn noSuchHostedZone(hostedZoneId)

    val maxItems = query("maxitems")?.toInt() ?: 1000
    val name = query("name")

    val matches = resources.forDomain(hostedZone.name)
        .sortedBy { it.name }
        .dropWhile { name != null && it.name < name }
        .toList()

    ListResourceRecordSetsResponse(
        maxItems = maxItems.toString(),
        nextRecordName = matches.drop(maxItems).firstOrNull()?.name,
        nextRecordType = matches.drop(maxItems).firstOrNull()?.type,
        nextRecordIdentifier = null,
        isTruncated = matches.size > maxItems,
        resourceRecordSets = matches.take(maxItems)
    ).asSuccess()
}

private fun serializeResponse(result: ListResourceRecordSetsResponse) = buildString {
    append("""<ListResourceRecordSetsResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">""")
    append("<IsTruncated>${result.isTruncated}</IsTruncated>")
    append("<MaxItems>${result.maxItems}</MaxItems>")
    if (result.nextRecordIdentifier != null) append("<NextRecordIdentifier>${result.nextRecordIdentifier}</NextRecordIdentifier>")
    if (result.nextRecordName != null) append("<NextRecordName>${result.nextRecordName}</NextRecordName>")
    if (result.nextRecordType != null) append("<NextRecordType>${result.nextRecordType}</NextRecordType>")
    append("<ResourceRecordSets>")
    result.resourceRecordSets.forEach { append(it.toXml()) }
    append("</ResourceRecordSets>")
    append("</ListResourceRecordSetsResponse>")
}
