package org.http4k.connect.amazon.route53.model

import org.http4k.connect.model.Timestamp
import org.http4k.connect.storage.Storage

data class StoredResource(
    val type: ResourceRecordSet.Type,
    val evaluateTargetHealth: Boolean?,
    val dnsName: String?,
    val ttl: Timestamp?,
    val values: List<String>?
)

fun Storage<StoredResource>.forDomain(domain: String) = keySet()
    .filter { it.endsWith(".$domain") }
    .mapNotNull { get(it) }
