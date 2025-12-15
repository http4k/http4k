package org.http4k.connect.amazon.scheduler

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.FakeAwsEnvironment
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.scheduler.model.ClientToken
import org.http4k.connect.amazon.scheduler.model.ScheduleName
import org.http4k.core.MockHttp
import org.http4k.core.Uri
import org.http4k.core.appendToPath
import org.http4k.core.query
import org.http4k.hamkrest.hasUri
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class HttpSchedulerTest {

    companion object {
        @JvmStatic
        fun requestUriSource() = listOf(
            Arguments.of(null, Uri.of("https://scheduler.ca-central-1.amazonaws.com/schedules")),
            Arguments.of(Uri.of("http://localhost:8080/schedules"), Uri.of("http://localhost:8080/schedules"))
        )
    }

    @ParameterizedTest
    @MethodSource("requestUriSource")
    fun `use correct request uri`(endpoint: Uri?, expectedRequestUri: Uri) {
        // given
        val mockHttp = MockHttp()
        val scheduler = Scheduler.Http(
            Region.CA_CENTRAL_1,
            CredentialsProvider.FakeAwsEnvironment(),
            mockHttp,
            overrideEndpoint = endpoint
        )

        // when
        val clientToken = ClientToken.random()
        runCatching {
            scheduler.deleteSchedule(ScheduleName.of("dummy"), null, clientToken)
        }

        // then
        assertThat(
            mockHttp.request,
            present(hasUri(expectedRequestUri.appendToPath("dummy").query("clientToken", clientToken.value)))
        )
    }
}
