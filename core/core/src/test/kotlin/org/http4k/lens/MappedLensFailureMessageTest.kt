package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MappedLensFailureMessageTest {

    @Test
    fun `uuid lens failure names the target type`() {
        val lens = Query.uuid().required("id")
        val failure = assertThrows<LensFailure> { lens(Request(GET, "/?id=not-a-uuid")) }
        assertThat(failure.message, equalTo("query 'id' must be uuid"))
    }

    @Test
    fun `localDate lens failure names the target type`() {
        val lens = Query.localDate().required("date")
        val failure = assertThrows<LensFailure> { lens(Request(GET, "/?date=not-a-date")) }
        assertThat(failure.message, equalTo("query 'date' must be date"))
    }
}
