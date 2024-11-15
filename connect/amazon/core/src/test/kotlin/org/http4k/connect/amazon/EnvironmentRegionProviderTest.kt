package org.http4k.connect.amazon

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.config.Environment.Companion.EMPTY
import org.http4k.connect.amazon.core.model.Region
import org.junit.jupiter.api.Test

class EnvironmentRegionProviderTest {

    @Test
    fun `region not in environment`() = assertThat(
        RegionProvider.Environment(EMPTY).invoke(),
        absent()
    )

    @Test
    fun `malformed region in environment`() = assertThat(
        RegionProvider.Environment(mapOf("AWS_REGION" to "nowhere")).invoke(),
        equalTo(Region.of("nowhere"))
    )

    @Test
    fun `region in environment`() = assertThat(
        RegionProvider.Environment(mapOf("AWS_REGION" to "ca-central-1")).invoke(),
        equalTo(Region.CA_CENTRAL_1)
    )
}
