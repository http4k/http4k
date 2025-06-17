package org.http4k.connect.amazon.route53.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.route53.model.Change

@Http4kConnectAction
class ChangeResourceRecordSets(
    val changes: List<Change>
) : Route53Action<Unit> {
}

