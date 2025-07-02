package org.http4k.connect.amazon.route53.endpoints

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import org.http4k.connect.amazon.route53.hostedZoneIdLens
import org.http4k.connect.amazon.route53.model.ListResourceRecordSetsResponse
import org.http4k.connect.amazon.route53.model.ResourceRecordSet
import org.http4k.connect.amazon.route53.model.StoredHostedZone
import org.http4k.connect.amazon.route53.model.forDomain
import org.http4k.connect.storage.Storage

fun listResourceRecordSets(
    hostedZones: Storage<StoredHostedZone>,
    resources: Storage<ResourceRecordSet>
) = route53FakeAction(ListResourceRecordSetsResponse::toXml) fn@{
    val hostedZone = hostedZones[hostedZoneIdLens(this).value] ?: return@fn noSuchHostedZone().asFailure()

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

private fun ListResourceRecordSetsResponse.toXml() = buildString {
    append("""<ListResourceRecordSetsResponse xmlns="https://route53.amazonaws.com/doc/2013-04-01/">""")
    append("<IsTruncated>${isTruncated}</IsTruncated>")
    append("<MaxItems>${maxItems}</MaxItems>")
    if (nextRecordIdentifier != null) append("<NextRecordIdentifier>${nextRecordIdentifier}</NextRecordIdentifier>")
    if (nextRecordName != null) append("<NextRecordName>${nextRecordName}</NextRecordName>")
    if (nextRecordType != null) append("<NextRecordType>${nextRecordType}</NextRecordType>")
    if (resourceRecordSets.isNotEmpty()) {
        append("<ResourceRecordSets>")
        resourceRecordSets.forEach { append(it.toXml()) }
        append("</ResourceRecordSets>")
    }
    append("</ListResourceRecordSetsResponse>")
}
