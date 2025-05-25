package org.http4k.bridge

import kotlinx.coroutines.runBlocking
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.net.URI

class SpringToHttp4kFallbackControllerSuccessTest {
    class Controller : SpringToHttp4kFallbackController({
        Response(Status.OK).body(it.body).headers(it.headers)
    })
    
    val mvc = MockMvcBuilders.standaloneSetup(Controller()).build()
    
    @Test
    fun `passes requests through and adapts to servlet`() = runBlocking {
        mvc.post(URI("/")) {
            header("header", "value")
            contentType = MediaType.TEXT_PLAIN
            content = "helloworld"
        }
            .andExpect { status { isOk() } }
            .andExpect { header { string("header", "value") } }
            .andExpect { content { string("helloworld") } }
    }
}
