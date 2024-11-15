package org.http4k.connect.amazon.instancemetadata

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.present
import org.http4k.connect.amazon.CredentialsChain
import org.http4k.core.Status.Companion.CONNECTION_REFUSED
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class Ec2CredentialsProviderTest {

    private val clock = Clock.fixed(Instant.parse("2022-03-04T12:00:00Z"), ZoneOffset.UTC)
    private val metadata = InstanceMetadata(clock.instant())
    private val service = FakeInstanceMetadataService(clock, metadata)
    private val chain = CredentialsChain.Ec2InstanceProfile(service, clock = clock)

    @Test
    fun `metadata service not available (not in EC2)`() {
        service.returnStatus(CONNECTION_REFUSED)

        assertThat(chain.invoke(), absent())
    }

    @Test
    fun `no instance profile available`() {
        val metadata = InstanceMetadata(clock.instant()).copy(profiles = emptySet())
        val service = FakeInstanceMetadataService(clock, metadata)
        val chain = CredentialsChain.Ec2InstanceProfile(service, clock = clock)

        assertThat(chain.invoke(), absent())
    }

    @Test
    fun `load credentials from profile`() {
        val credentials = chain.invoke()
        assertThat(credentials, present())

        assertThat(chain.invoke(), present())
        assertThat(metadata.credentials().entries, hasSize(equalTo(1)))
    }

    @Test
    fun `load cached credentials from profile`() {
        val credentials = chain.invoke()
        assertThat(credentials, present())

        assertThat(chain.invoke(), equalTo(credentials))

        assertThat(metadata.credentials().entries, hasSize(equalTo(1)))
    }
}
