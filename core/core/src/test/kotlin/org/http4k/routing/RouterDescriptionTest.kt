package org.http4k.routing

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status
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

    @Test
    fun `complicated toString`(approver: Approver) {
        val routes = reverseProxyRouting(
            "host" to routes("/foo" bind GET to { Response(Status.OK) }),
            "anotherHost" to routes("/bar" bind GET to { Response(Status.OK) }
            )
        )

        approver.assertApproved(routes.toString())
    }
}
