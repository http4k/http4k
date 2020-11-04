package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class ExtensionTests {

    @Test
    fun `alphabetise headers request`() {
        assertThat(
            Request(GET, "")
                .header("b", "b")
                .header("a", "c")
                .header("b", "a")
                .alphabetiseHeaders(),
            equalTo(
                Request(GET, "")
                    .header("a", "c")
                    .header("b", "a")
                    .header("b", "b")
            )
        )
    }

    @Test
    fun `alphabetise headers response`() {
        assertThat(
            Response(OK)
                .header("b", "b")
                .header("a", "c")
                .header("b", "a")
                .alphabetiseHeaders(),
            equalTo(
                Response(OK)
                    .header("a", "c")
                    .header("b", "a")
                    .header("b", "b")
            )
        )
    }
}
