package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.throws
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.junit.ResourceLoader
import org.http4k.junit.TestResources
import org.http4k.lens.Header.CONTENT_TYPE
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension

@ExtendWith(TestResources::class)
class ExampleBinaryApprovalTest {

    @RegisterExtension
    val approvalTest = BinaryApprovalTest(OCTET_STREAM)

    @Test
    fun `check response content`(approver: Approver, loader: ResourceLoader) {
        val app: HttpHandler = {
            Response(OK)
                .with(CONTENT_TYPE of OCTET_STREAM)
                .body(loader.stream("image.png"))
        }

        approver.assertApproved(app(Request(GET, "/url")))
    }

    @Test
    fun `check response content with mismatching content type`(approver: Approver) {
        assertThat({ approver.assertApproved(Response(OK)) }, throws<AssertionError>())
    }

    @Test
    fun `check request content`(approver: Approver, loader: ResourceLoader) {
        approver.assertApproved(
            Request(GET, "/url").with(CONTENT_TYPE of OCTET_STREAM)
                .body(loader.stream("image.png"))
        )
    }
}
