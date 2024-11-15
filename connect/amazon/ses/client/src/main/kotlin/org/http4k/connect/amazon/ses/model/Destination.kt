package org.http4k.connect.amazon.ses.model

data class Destination(
    val toAddresses: Set<EmailAddress>? = null,
    val ccAddresses: Set<EmailAddress>? = null,
    val bccAddresses: Set<EmailAddress>? = null
)

