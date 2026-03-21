import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.server.JettyLoom
import org.http4k.server.asServer
import org.http4k.wiretap.LocalTarget
import org.http4k.wiretap.Wiretap
import org.http4k.wiretap.junit.Intercept
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension

// Step 1: Just add the annotation — OTel captured automatically, HTML report on failure

fun MyHttp4kApp(
    http: HttpHandler = { req: Request -> Response(Status.OK) },
    otel: OpenTelemetry = GlobalOpenTelemetry.get()
) = { req: Request -> Response(Status.OK) }


@ExtendWith(Intercept::class)
class OtelCaptureTest {
    @Test
    fun `greets the user`() {
        MyHttp4kApp()(Request(GET, "/hello"))
    }
}


// Step 2: Wire up your app — full traffic capture and sequence diagrams


class TrafficCaptureTest {
    @RegisterExtension
    @JvmField
    val intercept = Intercept { MyHttp4kApp(http(), otel()) }

    @Test
    fun `greets the user`(http: HttpHandler) {
        http(Request(GET, "/hello"))
    }
}



// Step 3: One line to add the console to your running app

fun Foobar() {


    val wiretap = Wiretap(LocalTarget { MyHttp4kApp() })
    wiretap.asServer(JettyLoom(8080)).start()

}
