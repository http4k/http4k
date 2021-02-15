package guide.modules.approvaltests

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.http4k.testing.hasApprovedContent
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
        approver.assertApproved(app(Request(GET, "/url")))
    }

    @Test
    fun `check response content with expected status`(approver: Approver) {
        approver.assertApproved(app(Request(GET, "/url")), OK)
    }

    @Test
    fun `check request content`(approver: Approver) {
        approver.assertApproved(
            Request(GET, "/url").with(CONTENT_TYPE of APPLICATION_JSON).body("""{"message":"value"}""")
        )
    }

    @Test
    fun `combine approval with hamkrest matcher`(approver: Approver) {
        assertThat(app(Request(GET, "/url")), hasStatus(OK).and(approver.hasApprovedContent()))
    }
}
