package org.http4k.bridge

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import org.http4k.sse.SseEventId
import org.http4k.sse.SseMessage.Data
import org.http4k.sse.SseMessage.Event
import org.http4k.sse.SseMessage.Retry
import org.http4k.sse.SseResponse
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.net.URI
import java.time.Duration

class SpringToHttp4kSseControllerTest {

    class Controller : SpringToHttp4kSseController({ rq ->
        SseResponse { sse ->
            sse.send(Data("hello"))
            sse.send(Event("greet", "world", SseEventId("1")))
            sse.send(Retry(Duration.ofSeconds(2)))
            sse.send(Data("method=${rq.method} path=${rq.uri.path}"))
            sse.close()
        }
    })

    val mvc = MockMvcBuilders.standaloneSetup(Controller()).build()

    @Test
    fun `streams sse messages produced by the handler`() {
        val started = mvc.get(URI("/things")) {
            accept = TEXT_EVENT_STREAM
        }.andReturn()

        val body = mvc.perform(asyncDispatch(started))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(TEXT_EVENT_STREAM_VALUE))
            .andReturn().response.contentAsString

        assertThat(body, containsSubstring("data:hello"))
        assertThat(body, containsSubstring("event:greet"))
        assertThat(body, containsSubstring("data:world"))
        assertThat(body, containsSubstring("id:1"))
        assertThat(body, containsSubstring("retry:2000"))
        assertThat(body, containsSubstring("data:method=GET path=/things"))
    }

    @Test
    fun `accepts SSE on non-GET methods`() {
        val started = mvc.post(URI("/things")) {
            accept = TEXT_EVENT_STREAM
        }.andReturn()

        val body = mvc.perform(asyncDispatch(started))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(TEXT_EVENT_STREAM_VALUE))
            .andReturn().response.contentAsString

        assertThat(body, containsSubstring("data:method=POST path=/things"))
    }
}
