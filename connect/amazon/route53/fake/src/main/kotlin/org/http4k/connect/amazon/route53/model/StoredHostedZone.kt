package org.http4k.connect.amazon.route53.model

import org.http4k.connect.storage.Storage

data class StoredHostedZone(
    val id: HostedZoneId,
    val name: HostedZoneName,
    val callerReference: String,
    val config: Config?
)

fun StoredHostedZone.toHostedZone(resources: Storage<ResourceRecordSet>) = HostedZone(
    id = id,
    name = name,
    callerReference = callerReference,
    config = config,
    resourceRecordSetCount = resources.forDomain(name).count().toLong()
)
