package org.http4k.connect.amazon.route53.model

import org.http4k.connect.amazon.core.children
import org.http4k.connect.amazon.core.firstChild
import org.http4k.connect.amazon.core.firstChildText
import org.w3c.dom.Node

data class ResourceRecordSet(
    val name: String,
    val type: Type,
    val aliasTarget: AliasTarget?,
    val resourceRecords: List<String>?,
    val ttl: Int?
) {
    enum class Type { SOA, A, TXT, NS, CNAME, MX, NAPTR, PTR, SRV, SPF, AAAA, CAA, DS, TLSA, SSHFP, SVCB, HTTPS }

    fun toXml() = buildString {
        append("<ResourceRecordSet>")
        append("<Name>${name}</Name>")
        append("<Type>${type}</Type>")
        aliasTarget?.let { append(it.toXml()) }
        if (resourceRecords != null && resourceRecords.isNotEmpty()) {
            append("<ResourceRecords>")
            resourceRecords.forEach { append("<ResourceRecord><Value>$it</Value></ResourceRecord>") }
            append("</ResourceRecords>")
        }
        if (ttl != null) {
            append("<TTL>${ttl}</TTL>")
        }
        append("</ResourceRecordSet>")
    }

    companion object {
        fun parse(node: Node) = ResourceRecordSet(
            name = node.firstChildText("Name")!!,
            type = Type.valueOf(node.firstChildText("Type")!!),
            aliasTarget = node.firstChild("AliasTarget")?.let {
                AliasTarget(
                    dnsName = it.firstChildText("DNSName")!!,
                    hostedZoneId = HostedZoneId.parse(it.firstChildText("HostedZoneId")!!),
                    evaluateTargetHealth = it.firstChildText("EvaluateTargetHealth")!!.toBoolean()
                )
            },
            ttl = node.firstChildText("TTL")?.toInt(),
            resourceRecords = node.firstChild("ResourceRecords")
                ?.children("ResourceRecord")
                ?.map { it.firstChildText("Value")!! }
                ?.toList()
        )
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
        append("<HostedZoneId>${hostedZoneId}</HostedZoneId>")
        append("<EvaluateTargetHealth>${evaluateTargetHealth}</EvaluateTargetHealth>")
        append("</AliasTarget>")
    }
}
