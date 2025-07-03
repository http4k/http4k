package org.http4k.connect.amazon.route53.endpoints

import dev.forkhandles.result4k.asSuccess
import org.http4k.connect.amazon.core.children
import org.http4k.connect.amazon.core.firstChild
import org.http4k.connect.amazon.core.firstChildText
import org.http4k.connect.amazon.core.sequenceOfNodes
import org.http4k.connect.amazon.route53.hostedZoneIdLens
import org.http4k.connect.amazon.route53.model.AliasTarget
import org.http4k.connect.amazon.route53.model.Change
import org.http4k.connect.amazon.route53.model.ChangeInfo
import org.http4k.connect.amazon.route53.model.HostedZoneId
import org.http4k.connect.amazon.route53.model.ResourceRecordSet
import org.http4k.connect.amazon.route53.model.StoredHostedZone
import org.http4k.connect.amazon.route53.model.invalidChangeBatch
import org.http4k.connect.amazon.route53.model.noSuchHostedZone
import org.http4k.connect.amazon.route53.model.toXml
import org.http4k.connect.storage.Storage
import org.w3c.dom.Document
import java.time.Clock
import java.util.UUID

fun changeResourceRecordSets(
    hostedZones: Storage<StoredHostedZone>,
    resources: Storage<ResourceRecordSet>,
    clock: Clock
) = route53FakeAction(::parseRequest, ::serializeResponse) fn@{ changes ->
    val hostedZoneId = hostedZoneIdLens(this)
    val hostedZone = hostedZones[hostedZoneId.value] ?: return@fn noSuchHostedZone(hostedZoneId)

    // TODO validate data BEFORE, so that partial results aren't saved

    for (change in changes) {
        val record = change.resourceRecordSet.copy(
            name = change.resourceRecordSet.name.trimEnd('.').plus('.')
        )
        val key = "${record.type}:${record.name}"
        val exists = key in resources.keySet()
        val matches = record.name.endsWith(hostedZone.name.value)

        when(change.action) {
            // TODO handle particulars of what makes a valid CNAME, A, alias, etc.
            Change.Action.CREATE -> {
                if (!matches) return@fn invalidChangeBatch("[RRSet with DNS name ${record.name} is not permitted in zone ${hostedZone.name}]")
                if (exists) return@fn invalidChangeBatch("[Tried to create resource record set [name='${record.name}', type='${record.type}'] but it already exists]")
                resources[key] = record
            }
            Change.Action.UPSERT -> {
                if (!matches) return@fn invalidChangeBatch("[RRSet with DNS name ${record.name} is not permitted in zone ${hostedZone.name}]")
                resources[key] = record
            }
            Change.Action.DELETE -> {
                if (!exists) return@fn invalidChangeBatch("[Tried to delete resource record set [name='${record.name}', type='${record.type}'] but it was not found]")
                resources -= key
            }
        }
    }

    ChangeInfo(
        id = UUID.randomUUID().toString(),
        status = ChangeInfo.Status.INSYNC,
        submittedAt = clock.instant(),
        comment = null
    ).asSuccess()
}

private fun serializeResponse(change: ChangeInfo) =
    "<ChangeResourceRecordSetsResponse>${change.toXml()}</ChangeResourceRecordSetsResponse>"

private fun parseRequest(document: Document): List<Change> = document
    .getElementsByTagName("Change")
    .sequenceOfNodes()
    .map { node ->
        Change(
            action = Change.Action.valueOf(node.firstChildText("Action")!!),
            resourceRecordSet = node.firstChild("ResourceRecordSet")!!.let { set ->
                ResourceRecordSet(
                    name = set.firstChildText("Name")!!,
                    aliasTarget = set.firstChild("AliasTarget")?.let { target ->
                        AliasTarget(
                            dnsName = target.firstChildText("DNSName")!!,
                            hostedZoneId = HostedZoneId.parse(target.firstChildText("HostedZoneId")!!),
                            evaluateTargetHealth = target.firstChildText("EvaluateTargetHealth")!!.toBoolean(),
                        )
                    },
                    resourceRecords = set.firstChild("ResourceRecords")
                        ?.children("ResourceRecord")
                        ?.map { it.firstChildText("Value")!! }
                        ?.toList().orEmpty(),
                    ttl = set.firstChildText("TTL")?.toInt(),
                    type = ResourceRecordSet.Type.valueOf(set.firstChildText("Type")!!),
                )
            }
        )
    }
    .toList()
