package org.http4k.connect.storage

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class StorageExplorerTest {

    @Test
    fun `can serve API spec and UI`() {
        val explorer = Storage.InMemory<Entry>().asHttpHandler()
        assertThat(explorer(Request(GET, "/api/openapi")), hasStatus(OK))
        assertThat(explorer(Request(GET, "/")), hasStatus(OK) and hasBody(containsSubstring("html")))
    }

    data class Entry(val value: String)
}
