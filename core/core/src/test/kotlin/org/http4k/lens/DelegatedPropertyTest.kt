package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with
import org.junit.jupiter.api.Test

class DelegatedPropertyTest {
    private val qReq by Query.long().of().required()
    private val qOpt by Query.long().of().optional()
    private val qDef by Query.long().of().defaulted(999)
    private val qListReq by Query.long().multi.of().required()

    @Test
    fun `extract and inject`() {
        val base = Request(GET, "")
        assertThat(qReq(base.query("qReq", "123")), equalTo(123L))
        assertThat(base.with(qReq of 123), equalTo(base.query("qReq", "123")))

        assertThat(qDef(base), equalTo(999))

        assertThat(qOpt(base.query("qOpt", "123")), equalTo(123L))
        assertThat(base.with(qOpt of 123), equalTo(base.query("qOpt", "123")))

        assertThat(qListReq(base.query("qListReq", "123").query("qListReq", "456")), equalTo(listOf(123L, 456L)))
        assertThat(
            base.with(qListReq of listOf(123L, 456L)),
            equalTo(base.query("qListReq", "123").query("qListReq", "456"))
        )
    }
}
