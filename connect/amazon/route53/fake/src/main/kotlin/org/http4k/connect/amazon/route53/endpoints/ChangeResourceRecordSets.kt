package org.http4k.connect.amazon.route53.endpoints

import org.http4k.connect.amazon.core.children
import org.http4k.connect.amazon.core.firstChild
import org.http4k.connect.amazon.core.firstChildText
import org.http4k.connect.amazon.core.sequenceOfNodes
import org.http4k.connect.amazon.core.xmlDoc
import org.http4k.connect.amazon.route53.hostedZoneIdLens
import org.http4k.connect.amazon.route53.model.AliasTarget
import org.http4k.connect.amazon.route53.model.Change
import org.http4k.connect.amazon.route53.model.ChangeInfo
import org.http4k.connect.amazon.route53.model.HostedZoneId
import org.http4k.connect.amazon.route53.model.ResourceRecord
import org.http4k.connect.amazon.route53.model.ResourceRecordSet
import org.http4k.connect.amazon.route53.model.StoredHostedZone
import org.http4k.connect.amazon.route53.model.StoredResource
import org.http4k.connect.amazon.route53.model.toXml
import org.http4k.connect.model.Timestamp
import org.http4k.connect.storage.Storage
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.contentType
import java.time.Clock
import java.util.UUID

fun changeResourceRecordSets(hostedZones: Storage<StoredHostedZone>, records: Storage<StoredResource>, clock: Clock) = { request: Request ->
    val hostedZoneId = hostedZoneIdLens(request)
    val changes = request.body.parse()

    // TODO verify hostedzone exists
    // TODO verify fully qualified DNS names match hostedZoneId

    for (change in changes) {
        when(change.action) {
            Change.Action.CREATE, Change.Action.UPSERT -> { // TODO handle create/upsert differences
                records[change.resourceRecordSet.name] = StoredResource(
                    type = change.resourceRecordSet.type,
                    evaluateTargetHealth = change.resourceRecordSet.aliasTarget?.evaluateTargetHealth,
                    dnsName = change.resourceRecordSet.name,
                    ttl = change.resourceRecordSet.ttl,
                    values = change.resourceRecordSet.resourceRecords?.map { it.value }
                )
            }
            Change.Action.DELETE -> { // TODO see if the resource must exist
                records.remove(change.resourceRecordSet.name)
            }
        }
    }

    val result = ChangeInfo(
        id = UUID.randomUUID().toString(),
        status = ChangeInfo.Status.INSYNC,
        submittedAt = clock.instant(),
        comment = null
    )

    Response(Status.OK)
        .contentType(ContentType.APPLICATION_XML)
        .body("""<?xml version="1.0" encoding="UTF-8"?>
<ChangeResourceRecordSetsResponse>
    ${result.toXml()}
</ChangeResourceRecordSetsResponse>
""")
}

private fun Body.parse(): Sequence<Change> = xmlDoc()
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
                    resourceRecords = set.children("ResourceRecord").map { record ->
                        ResourceRecord(
                            value = record.firstChildText("Value")!!
                        )
                    }.toList(),
                    ttl = Timestamp.parse(set.firstChildText("TTL")!!),
                    type = ResourceRecordSet.Type.valueOf(set.firstChildText("Type")!!),
                )
            }
        )
    }
