package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Status.Companion.CONNECTION_REFUSED
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class StatusTest {

    @Test
    fun `can override description`() {
        val description = OK.description("all good")
        assertThat(description.description, equalTo("all good"))
        assertThat(description.toString(), equalTo("200 all good"))
    }

    @Test
    fun `equality does not include description`() {
        assertThat(CONNECTION_REFUSED.description("foo") == CONNECTION_REFUSED.description("bar"), equalTo(true))
    }

    @Test
    fun `use default description when there is no matching description for the status code`() {
        val status = Status(510, null)

        assertThat(status.description, equalTo("No description"))
    }
}
