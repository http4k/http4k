package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Status.Companion.CONNECTION_REFUSED
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
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
        assertThat(CONNECTION_REFUSED.description("foo") == SERVICE_UNAVAILABLE.description("foo"), equalTo(false))
    }

    @Test
    fun `hashcode does not include description`() {
        assertThat(CONNECTION_REFUSED.description("foo").hashCode() == CONNECTION_REFUSED.description("bar").hashCode(), equalTo(true))
        assertThat(CONNECTION_REFUSED.description("foo").hashCode() == SERVICE_UNAVAILABLE.description("foo").hashCode(), equalTo(false))
    }

    @Test
    fun `use default description when there is no matching description for the status code`() {
        val status = Status(510, null)

        assertThat(status.description, equalTo("No description"))
    }
}
