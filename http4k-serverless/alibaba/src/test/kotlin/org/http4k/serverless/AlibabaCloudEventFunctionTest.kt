package org.http4k.serverless

import com.aliyun.fc.runtime.Context
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.util.proxy
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

data class Foo(val name: String)

class AlibabaCloudEventFunctionTest {
    @Test
    fun `calls the handler and returns proper body`() {

        val response = ByteArrayOutputStream()

        object : AlibabaCloudEventFunction(FnLoader {
            FnHandler { input: Foo, _: Context ->
                "${input.name} alibaba"
            }
        }) {}.handleRequest("""{"name":"hello"}""".byteInputStream(), response, proxy())

        assertThat(response.toString(), equalTo("hello alibaba"))
    }
}
