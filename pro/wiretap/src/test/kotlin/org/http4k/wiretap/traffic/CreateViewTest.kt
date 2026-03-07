package org.http4k.wiretap.traffic

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.testing.Approver
import org.http4k.wiretap.HttpWiretapFunctionContract
import org.http4k.wiretap.McpWiretapFunctionContract
import org.http4k.wiretap.domain.ViewStore
import org.junit.jupiter.api.Test

class CreateViewTest : HttpWiretapFunctionContract, McpWiretapFunctionContract {

    override val toolName = "create_view"

    private val views = ViewStore.InMemory()

    override val function = CreateView(views)

    @Test
    fun `http creates view`(approver: Approver) {
        val sizeBefore = views.list().size

        approver.assertApproved(
            httpClient()(
                Request(POST, "/views")
                    .body("""{"name":"API","direction":"","host":"","method":"","status":"","path":"/api"}""")
            )
        )

        assertThat(views.list().size, equalTo(sizeBefore + 1))
    }

    @Test
    fun `mcp creates view`(approver: Approver) {
        val sizeBefore = views.list().size

        approver.assertToolResponse(mapOf("name" to "API", "path" to "/api"))

        assertThat(views.list().size, equalTo(sizeBefore + 1))
    }
}
