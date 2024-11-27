package org.http4k.connect.amazon.instancemetadata

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.RegionProvider
import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.Status
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class Ec2InstanceProfileRegionProviderTest {

    private val clock = Clock.fixed(Instant.parse("2022-03-04T12:00:00Z"), ZoneOffset.UTC)
    private val metadata = InstanceMetadata(clock.instant(), region = Region.CA_CENTRAL_1)
    private val service = FakeInstanceMetadataService(clock, metadata)
    private val provider = RegionProvider.Ec2InstanceProfile(service)

    @Test
    fun `metadata service not available (not in EC2)`() {
        service.returnStatus(Status.CONNECTION_REFUSED)

        assertThat(provider.invoke(), absent())
    }

    @Test
    fun `load region from instance profile`() = assertThat(
        provider.invoke(),
        equalTo(Region.CA_CENTRAL_1)
    )
}
