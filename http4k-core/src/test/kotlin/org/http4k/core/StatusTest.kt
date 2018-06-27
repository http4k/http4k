package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class StatusTest {
    @Test
    fun `can override description`() {
        assertThat(Status.OK.description("all good").description, equalTo("all good"))
    }
}