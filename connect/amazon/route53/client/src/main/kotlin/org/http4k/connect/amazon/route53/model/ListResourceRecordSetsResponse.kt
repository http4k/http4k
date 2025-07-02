package org.http4k.connect.amazon.route53.model

import org.http4k.connect.amazon.core.children
import org.http4k.connect.amazon.core.firstChild
import org.http4k.connect.amazon.core.firstChildText
import org.w3c.dom.Document

data class ListResourceRecordSetsResponse(
    val isTruncated: Boolean,
    val maxItems: String,
    val nextRecordIdentifier: String?,
    val nextRecordName: String?,
    val nextRecordType: ResourceRecordSet.Type?,
    val resourceRecordSets: List<ResourceRecordSet>
) {
    companion object {
        fun parse(document: Document) = document.getElementsByTagName("ListResourceRecordSetsResponse")
            .item(0)!!
            .let { node ->
                ListResourceRecordSetsResponse(
                    isTruncated = node.firstChildText("IsTruncated")!!.toBoolean(),
                    maxItems = node.firstChildText("MaxItems")!!,
                    nextRecordIdentifier = node.firstChildText("NextRecordIdentifier"),
                    nextRecordName = node.firstChildText("NextRecordName"),
                    nextRecordType = node.firstChildText("NextRecordType")?.let(ResourceRecordSet.Type::valueOf),
                    resourceRecordSets = node.firstChild("ResourceRecordSets")!!
                        .children("ResourceRecordSet")
                        .map(ResourceRecordSet::parse)
                        .toList()
                )
            }
    }
}
