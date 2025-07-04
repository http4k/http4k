package org.http4k.connect.amazon.route53.model

import org.http4k.connect.amazon.core.firstChildText
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.core.model.VpcId
import org.w3c.dom.Node

data class VPC(
    val vpcId: VpcId,
    val vpcRegion: Region
) {
    fun toXml() = "<VPC><VPCId>$vpcId</VPCId><VPCRegion>$vpcRegion</VPCRegion></VPC>"

    companion object {
        fun parse(node: Node) = VPC(
            vpcId = VpcId.parse(node.firstChildText("VPCId")!!),
            vpcRegion = Region.parse(node.firstChildText("VPCRegion")!!)
        )
    }
}
