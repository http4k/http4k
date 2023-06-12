package org.http4k.contract.ui

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class RedocLiteTest {

    @Test
    fun `can serve redoc lite`(approver: Approver) {
        val handler = redocLite {
            url = "spec"
            pageTitle = "Cat Shelter"
            options["foo"] = "bar"
            options["toll"] = "troll"
        }
        approver.assertApproved(handler(Request(Method.GET, "")))
    }
}
