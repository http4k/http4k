package org.http4k.connect.amazon.route53.model

data class Change(
    val action: Action,
    val resourceRecordSet: ResourceRecordSet
) {
    enum class Action { CREATE, DELETE, UPSERT }

    internal fun toXml() = buildString {
        append("<Change>")
        append("<Action>${action}</Action>")
        append(resourceRecordSet.toXml())
        append("</Change>")
    }
}

