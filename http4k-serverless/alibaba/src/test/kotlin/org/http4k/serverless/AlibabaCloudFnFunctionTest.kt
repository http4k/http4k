package org.http4k.serverless

import com.aliyun.fc.runtime.Context
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.servlet.FakeHttpServletRequest
import org.http4k.servlet.FakeHttpServletResponse
import org.http4k.util.proxy
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

data class Foo(val name: String)

class AlibabaCloudFnFunctionTest {
    @Test
    fun `calls the handler and returns proper body`() {

        val response = ByteArrayOutputStream()

        object : AlibabaCloudFnFunction(FnLoader {
            FnHandler {
                    input: Foo, _: Context ->
                "${input.name} alibaba"
            }
        }) {}.handleRequest("""{"name":"hello"}""".byteInputStream(), response, proxy())

        assertThat(response.toString(), equalTo("hello alibaba"))
    }
}
