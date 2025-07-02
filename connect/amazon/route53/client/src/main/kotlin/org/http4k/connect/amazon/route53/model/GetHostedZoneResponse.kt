package org.http4k.connect.amazon.route53.model

import org.http4k.connect.amazon.core.firstChild
import org.http4k.connect.amazon.core.sequenceOfNodes
import org.w3c.dom.Document

data class GetHostedZoneResponse(
    val delegationSet: DelegationSet?,
    val hostedZone: HostedZone,
    val vpcs: List<VpcConfig>
) {
    companion object {
        fun parse(document: Document) = document
            .getElementsByTagName("HostedZoneResponse")
            .item(0)
            .let { node ->
                GetHostedZoneResponse(
                    hostedZone = HostedZone.parse(node.firstChild("HostedZone")!!),
                    delegationSet = node.firstChild("DelegationSet")?.let(DelegationSet::parse),
                    vpcs = node.firstChild("VPCs")!!.childNodes.sequenceOfNodes().map(VpcConfig::parse).toList()
                )
            }
    }
}
