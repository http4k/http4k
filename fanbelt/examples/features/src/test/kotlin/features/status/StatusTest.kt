package features.status

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.fanbelt.testing.TestTenant
import org.http4k.fanbelt.testing.infrastructure.TestRuntime
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import java.time.Duration.ofMinutes

class StatusTest {
    private val says = mutableMapOf<String, String>()
    private val statusPage = routes("/status/{tenant}" bind GET to { req ->
        says[req.path("tenant")]?.let { Response(OK).body(it) } ?: Response(SERVICE_UNAVAILABLE)
    })
    private val runtime = TestRuntime(http = statusPage)
    private val tenant = TestTenant(runtime)
    private val status = StatusFeature(runtime.http, runtime.clock)

    @Test
    fun `shows what the status service says about this tenant`() {
        says["acme"] = "All systems go"

        assertThat(tenant { status.current.read() }, equalTo(ServiceStatus("All systems go")))
    }

    @Test
    fun `a change in the service shows once the state asks again`() {
        says["acme"] = "All systems go"
        tenant { status.current.read() }

        says["acme"] = "Payments are slow"
        tenant { tick(ofMinutes(1)) }

        assertThat(tenant { status.current.read() }, equalTo(ServiceStatus("Payments are slow")))
    }

    @Test
    fun `a service that can't answer shows as unknown`() {
        assertThat(tenant { status.current.read() }, equalTo(ServiceStatus.UNKNOWN))
    }
}
