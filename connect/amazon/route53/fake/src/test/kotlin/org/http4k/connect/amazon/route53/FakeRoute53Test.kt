package org.http4k.connect.amazon.route53

import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.util.FixedClock
import route53.Route53Contract

class FakeRoute53Test: Route53Contract, FakeAwsContract {
    override val http = FakeRoute53(FixedClock)
}
