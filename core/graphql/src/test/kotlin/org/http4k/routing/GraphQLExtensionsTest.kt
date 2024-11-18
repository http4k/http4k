package org.http4k.routing

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.testing.Approver
import org.http4k.testing.HtmlApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(HtmlApprovalTest::class)
class GraphQLExtensionsTest {

    @Test
    fun `can serve playground`(approver: Approver) {
        approver.assertApproved(graphQLPlayground(Uri.of("/graphql"))(Request(GET, "")))
    }
}
