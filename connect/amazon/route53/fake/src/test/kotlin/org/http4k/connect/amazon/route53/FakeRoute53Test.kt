package org.http4k.connect.amazon.route53

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.core.model.VpcId
import org.http4k.connect.amazon.route53.action.createHostedZone
import org.http4k.connect.amazon.route53.action.deleteHostedZone
import org.http4k.connect.amazon.route53.action.getHostedZone
import org.http4k.connect.amazon.route53.model.HostedZoneName
import org.http4k.connect.amazon.route53.model.VPC
import org.http4k.connect.successValue
import org.http4k.util.FixedClock
import org.junit.jupiter.api.Test
import java.util.UUID

class FakeRoute53Test: Route53Contract, FakeAwsContract {
    override val http = FakeRoute53(FixedClock)

    @Test
    fun `create and get hosted zone with vpc`() {
        val vpcConfig = VPC(
            vpcId = VpcId.parse("vpc123"),
            vpcRegion = Region.CA_CENTRAL_1
        )

        val result = route53.createHostedZone(
            name = HostedZoneName.parse("${UUID.randomUUID()}.com"),
            callerReference = UUID.randomUUID().toString(),
            delegationSetId = null,
            hostedZoneConfig = null,
            vpc = vpcConfig
        ).successValue()

        try {
            assertThat(result.vpc, equalTo(vpcConfig))

            val retrieved = route53.getHostedZone(result.hostedZone.id).successValue()
            assertThat(retrieved.hostedZone, equalTo(result.hostedZone))
            assertThat(retrieved.vpcs, hasSize(equalTo(1)))
            assertThat(retrieved.vpcs.first(), equalTo(vpcConfig))
        } finally {
            route53.deleteHostedZone(result.hostedZone.id).successValue()
        }
    }

    // TODO add support for add/remove vpc associations
}
