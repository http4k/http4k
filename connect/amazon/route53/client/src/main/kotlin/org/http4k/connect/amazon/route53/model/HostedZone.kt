package org.http4k.connect.amazon.route53.model

import org.http4k.connect.amazon.core.firstChild
import org.http4k.connect.amazon.core.firstChildText
import org.w3c.dom.Node

data class HostedZone(
    val callerReference: String,
    val id: HostedZoneId,
    val name: String,
    val config: Config?,
//    val linkedService:
    val resourceRecordSetCount: Long?
) {
    companion object {
        fun parse(node: Node) = HostedZone(
            name = node.firstChildText("Name")!!,
            id = HostedZoneId.parse(node.firstChildText("Id")!!),
            callerReference = node.firstChildText("CallerReference")!!,
            config = node.firstChild("Config")?.let { config ->
                Config(
                    comment = config.firstChildText("Comment"),
                    privateZone = config.firstChildText("PrivateZone")?.toBoolean()
                )
            },
            resourceRecordSetCount = node.firstChildText("ResourceRecordSetCount")?.toLong()
        )
    }
}
