package org.http4k.contract.openapi

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class ApiRendererTest {

    @Test
    fun `can convert any api renderer to cached form`() {
        var called = 0
        val delegate = object : ApiRenderer<Any, Any> {
            override fun api(api: Any): Any = apply { called++ }
            override fun toSchema(obj: Any, overrideDefinitionId: String?, prefix: String?) = TODO("not implemented")
        }.cached()

        delegate.api(Unit)
        delegate.api(Unit)

        assertThat(called, equalTo(1))
    }
}
