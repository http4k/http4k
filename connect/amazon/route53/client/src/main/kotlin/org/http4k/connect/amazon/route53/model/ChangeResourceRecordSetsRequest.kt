package org.http4k.connect.amazon.route53.model

import org.http4k.connect.model.Timestamp

data class Change(
    val action: Action,
    val resourceRecordSet: ResourceRecordSet
) {
    enum class Action { CREATE, DELETE, UPSERT }

    internal fun toXml() = buildString {
        append("<Change>")
        append("<Action>${action}</Action>")
        append("<ResourceRecordSet>${resourceRecordSet.toXml()}</ResourceRecordSet>")
        append("</Change>")
    }
}

data class ResourceRecordSet(
    val name: String,
    val type: Type,
    val aliasTarget: AliasTarget?,
    val resourceRecords: List<ResourceRecord>?,
    val ttl: Timestamp?
) {
    enum class Type { SOA, A, TXT, NS, CNAME, MX, NAPTR, PTR, SRV, SPF, AAAA, CAA, DS, TLSA, SSHFP, SVCB, HTTPS }

    internal fun toXml() = buildString {
        append("<Name>${name}</Name>")
        append("<Type>${type}</Type>")
        aliasTarget?.let { append(it.toXml()) }
        append("<ResourceRecords>")
        resourceRecords?.forEach { append(it.toXml()) }
        append("</ResourceRecords>")
        append("<TTL>${ttl}</TTL>")
    }
}

data class AliasTarget(
    val dnsName: String,
    val hostedZoneId: HostedZoneId,
    val evaluateTargetHealth: Boolean
) {
    internal fun toXml() = buildString {
        append("<AliasTarget>")
        append("<DNSName>${dnsName}</DNSName>")
        append("HostedZoneId>${hostedZoneId}</HostedZoneId>")
        append("<EvaluateTargetHealth>${evaluateTargetHealth}</EvaluateTargetHealth>")
        append("</AliasTarget>")
    }
}

data class ResourceRecord(
    val value: String
) {
    internal fun toXml() = "<ResourceRecord><Value>${value}</Value></ResourceRecord>"
}
