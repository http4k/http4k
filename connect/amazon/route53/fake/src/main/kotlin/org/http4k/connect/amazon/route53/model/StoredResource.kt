package org.http4k.connect.amazon.route53.model

import org.http4k.connect.model.Timestamp

data class StoredResource(
    val type: ResourceRecordSet.Type,
    val evaluateTargetHealth: Boolean?,
    val dnsName: String?,
    val ttl: Timestamp?,
    val values: List<String>?
)

