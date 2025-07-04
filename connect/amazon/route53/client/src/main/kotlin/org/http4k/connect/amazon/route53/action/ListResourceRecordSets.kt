package org.http4k.connect.amazon.route53.action

import dev.forkhandles.result4k.Result4k
import org.http4k.connect.Action
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.route53.model.HostedZoneId
import org.http4k.connect.amazon.route53.model.ListResourceRecordSetsResponse
import org.http4k.connect.amazon.route53.model.ResourceRecordSet
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.query

@Http4kConnectAction
class ListResourceRecordSets(
    val hostedZoneId: HostedZoneId,
    val startRecordIdentifier: String?,
    val maxItems: Int?,
    val name: String?,
    val type: ResourceRecordSet.Type?,
) : Route53Action<ListResourceRecordSetsResponse>(
        method = Method.GET,
        uri = Uri.of("/2013-04-01/hostedzone/$hostedZoneId/rrset")
            .queryIfPresent("identifier", startRecordIdentifier)
            .queryIfPresent("maxitems", maxItems?.toString())
            .queryIfPresent("name", name)
            .queryIfPresent("type", type?.toString()),
        successFn = ListResourceRecordSetsResponse::parse
    ), Action<Result4k<ListResourceRecordSetsResponse, RemoteFailure>> {

    override fun toXml() = ""
}

private fun Uri.queryIfPresent(name: String, value: String?) = value?.let { query(name, it) } ?: this
