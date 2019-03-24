package guide.modules.approvaltests


import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.invoke
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class ExampleApprovalTest {

    private val app: HttpHandler = { Response(OK).body("hello world") }

    @Test
    fun `check response content`(approver: Approver) {
        approver {
            app(Request(GET, "/url"))
        }
    }

    @Test
    fun `check response content with matcher`(approver: Approver) {
        approver(hasStatus(OK)) {
            app(Request(GET, "/url"))
        }
    }

    @Test
    fun `check request content`(approver: Approver) {
        approver {
            Request(GET, "/url").body("foobar")
        }
    }
}
