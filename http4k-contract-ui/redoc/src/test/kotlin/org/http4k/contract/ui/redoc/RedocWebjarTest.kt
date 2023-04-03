package org.http4k.contract.ui.redoc

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class RedocWebjarTest {

    @Test // regression test for issue 880
    fun `base path redirects to index`(approver: Approver) {
        val handler = routes(
            "ui" bind redocWebjar()
        )
        assertThat(handler(Request(Method.GET, "ui")), hasStatus(Status.FOUND).and(hasHeader("Location", "ui/index.html")))
    }

    @Test
    fun `can serve redoc webjar`(approver: Approver) {
        val handler = redocWebjar {
            url = "spec"
            pageTitle = "Cat Shelter"
            options["foo"] = "bar"
            options["toll"] = "troll"
        }
        approver.assertApproved(handler(Request(Method.GET, "index.html")))
    }
}

