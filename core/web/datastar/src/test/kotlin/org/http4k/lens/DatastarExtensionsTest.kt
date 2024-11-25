package org.http4k.lens

import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class DatastarExtensionsTest {

    @Test
    fun `can inject a datastar fragment into a response`(approver: Approver) {
        approver.assertApproved(Response(Status.OK).datastar("foo", "bar"))
    }
}
