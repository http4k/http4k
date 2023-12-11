package org.http4k.routing

import org.http4k.core.Method.GET
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class RouterDescriptionTest {

    @Test
    fun `toString is friendly`(approver: Approver) {
        val and = GET.and(headers("host"))

        approver.assertApproved(and.and(headers("host")).toString())
    }
}
