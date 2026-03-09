/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.traffic

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.DELETE
import org.http4k.core.Request
import org.http4k.testing.Approver
import org.http4k.wiretap.HttpWiretapFunctionContract
import org.http4k.wiretap.McpWiretapFunctionContract
import org.http4k.wiretap.domain.TransactionFilter
import org.http4k.wiretap.domain.ViewStore
import org.junit.jupiter.api.Test

class DeleteViewTest : HttpWiretapFunctionContract, McpWiretapFunctionContract {

    override val toolName = "delete_view"

    private val views = ViewStore.InMemory()

    override val function = DeleteView(views)

    @Test
    fun `http deletes view`(approver: Approver) {
        val view = views.add("Test", TransactionFilter())
        val sizeBefore = views.list().size

        approver.assertApproved(httpClient()(Request(DELETE, "/views/${view.id}")))

        assertThat(views.list().size, equalTo(sizeBefore - 1))
        assertThat(views.list().none { it.id == view.id }, equalTo(true))
    }

    @Test
    fun `mcp deletes view`(approver: Approver) {
        val view = views.add("Test", TransactionFilter())
        val sizeBefore = views.list().size

        approver.assertToolResponse(mapOf("id" to view.id))

        assertThat(views.list().size, equalTo(sizeBefore - 1))
        assertThat(views.list().none { it.id == view.id }, equalTo(true))
    }
}
