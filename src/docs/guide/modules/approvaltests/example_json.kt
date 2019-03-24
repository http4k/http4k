package guide.modules.approvaltests

import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Header
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.invoke
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class ExampleJsonApprovalTest {

    private val app: HttpHandler = {
        Response(OK)
            .with(CONTENT_TYPE of APPLICATION_JSON)
            .body("""{"message":"value"}""")
    }

    @Test
    fun `check response content`(approver: Approver) {
        approver {
            app(Request(Method.GET, "/url"))
        }
    }

    @Test
    fun `check response content with hamkrest matcher`(approver: Approver) {
        approver(hasStatus(Status.OK)) {
            app(Request(Method.GET, "/url"))
        }
    }

    @Test
    fun `check request content`(approver: Approver) {
        approver {
            Request(Method.GET, "/url").with(Header.CONTENT_TYPE of ContentType.APPLICATION_JSON).body("""{"message":"value"}""")
        }
    }
}