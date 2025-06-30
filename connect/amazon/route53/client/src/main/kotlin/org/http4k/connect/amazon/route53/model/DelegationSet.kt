package org.http4k.connect.amazon.route53.model

data class DelegationSet(
    val nameServers: List<String>,
    val callerReference: String?,
    val id: String?,
)
