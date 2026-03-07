package org.http4k.wiretap.traffic

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.testing.Approver
import org.http4k.wiretap.HttpWiretapFunctionContract
import org.http4k.wiretap.McpWiretapFunctionContract
import org.http4k.wiretap.domain.TransactionFilter
import org.http4k.wiretap.domain.ViewStore
import org.junit.jupiter.api.Test

class UpdateViewTest : HttpWiretapFunctionContract, McpWiretapFunctionContract {

    override val toolName = "update_view"

    private val views = ViewStore.InMemory()

    override val function = UpdateView(views)

    @Test
    fun `http updates view`(approver: Approver) {
        val view = views.add("Test", TransactionFilter())

        approver.assertApproved(
            httpClient()(
                Request(PUT, "/views/${view.id}")
                    .body("""{"direction":"","host":"","method":"","status":"","path":"/updated"}""")
            )
        )

        assertThat(views.list().find { it.id == view.id }?.filter?.path, equalTo("/updated"))
    }

    @Test
    fun `mcp updates view`(approver: Approver) {
        val view = views.add("Test", TransactionFilter())

        approver.assertToolResponse(mapOf("id" to view.id, "path" to "/updated"))

        assertThat(views.list().find { it.id == view.id }?.filter?.path, equalTo("/updated"))
    }
}
