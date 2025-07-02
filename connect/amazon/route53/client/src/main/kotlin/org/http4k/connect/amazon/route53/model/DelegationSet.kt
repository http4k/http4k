package org.http4k.connect.amazon.route53.model

import org.http4k.connect.amazon.core.children
import org.http4k.connect.amazon.core.firstChildText
import org.w3c.dom.Node

data class DelegationSet(
    val nameServers: List<String>,
    val callerReference: String?,
    val id: String?,
) {
    companion object {
        fun parse(node: Node) = DelegationSet(
            nameServers = node.children("NameServers")
                .map { it.firstChildText("NameServer")!! }
                .toList()
            ,
            callerReference = node.firstChildText("CallerReference")!!,
            id = node.firstChildText("Id")!!
        )
    }
}
