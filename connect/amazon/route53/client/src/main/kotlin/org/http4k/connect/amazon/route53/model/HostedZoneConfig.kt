package org.http4k.connect.amazon.route53.model

data class HostedZoneConfig(
    val comment: String?,
    val privateZone: Boolean?
) {
    fun toXml() = buildString {
        append("<HostedZoneConfig>")
        if (comment != null) append("""<Comment>${comment}</Comment>""")
        if (privateZone != null) append("""<PrivateZone>${privateZone}</PrivateZone>""")
        append("</HostedZoneConfig>")
    }
}
