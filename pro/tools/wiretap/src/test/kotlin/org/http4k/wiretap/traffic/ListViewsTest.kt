package org.http4k.wiretap.traffic

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.testing.Approver
import org.http4k.wiretap.HttpWiretapFunctionContract
import org.http4k.wiretap.McpWiretapFunctionContract
import org.http4k.wiretap.domain.ViewStore
import org.junit.jupiter.api.Test

class ListViewsTest : HttpWiretapFunctionContract, McpWiretapFunctionContract {

    override val toolName = "list_views"

    private val views = ViewStore.InMemory()

    override val function = ListViews(views)

    @Test
    fun `http lists views`(approver: Approver) {
        approver.assertApproved(httpClient()(Request(GET, "/views")))
    }

    @Test
    fun `mcp lists views`(approver: Approver) {
        approver.assertToolResponse(emptyMap())
    }
}
