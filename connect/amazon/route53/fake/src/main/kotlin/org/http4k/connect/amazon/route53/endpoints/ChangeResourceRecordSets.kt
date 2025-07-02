package org.http4k.connect.amazon.route53.endpoints

import dev.forkhandles.result4k.asFailure
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
import org.http4k.connect.amazon.route53.model.ResourceRecord
import org.http4k.connect.amazon.route53.model.ResourceRecordSet
import org.http4k.connect.amazon.route53.model.StoredHostedZone
import org.http4k.connect.amazon.route53.model.forDomain
import org.http4k.connect.amazon.route53.model.toXml
import org.http4k.connect.storage.Storage
import org.w3c.dom.Document
import java.time.Clock
import java.util.UUID

fun changeResourceRecordSets(
    hostedZones: Storage<StoredHostedZone>,
    resources: Storage<ResourceRecordSet>,
    clock: Clock
) = route53FakeAction(
    requestBodyFn = Document::fromXml,
    successFn = { "<ChangeResourceRecordSetsResponse>${it.toXml()}</ChangeResourceRecordSetsResponse>" }
) fn@{ changes ->
    val hostedZoneId = hostedZoneIdLens(this)
    val hostedZone = hostedZones[hostedZoneId.value] ?: return@fn noSuchHostedZone().asFailure()

    for (change in changes) {
        when(change.action) {
            // TODO verify fully qualified DNS names match hostedZoneId
            //  TODO handle create/upsert differences
            Change.Action.CREATE, Change.Action.UPSERT -> {
                resources[change.resourceRecordSet.name] = change.resourceRecordSet
            }
            Change.Action.DELETE -> {
                if (change.resourceRecordSet !in resources.forDomain(hostedZone.name)) {
                    return@fn invalidChangeBatch().asFailure()
                }
                resources.remove(change.resourceRecordSet.name)
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

private fun Document.fromXml(): List<Change> = this
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
                        ?.map { ResourceRecord(value = it.firstChildText("Value")!!) }
                        ?.toList().orEmpty(),
                    ttl = set.firstChildText("TTL")?.toInt(),
                    type = ResourceRecordSet.Type.valueOf(set.firstChildText("Type")!!),
                )
            }
        )
    }
    .toList()
