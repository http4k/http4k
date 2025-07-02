package org.http4k.connect.amazon.route53.model

data class Config(
    val comment: String?,
    val privateZone: Boolean?
) {
    fun toXml() = buildString {
        append("<Config>")
        if (comment != null) append("""<Comment>${comment}</Comment>""")
        if (privateZone != null) append("""<PrivateZone>${privateZone}</PrivateZone>""")
        append("</Config>")
    }
}
