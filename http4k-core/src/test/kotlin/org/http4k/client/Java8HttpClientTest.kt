package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.http4k.core.BodyMode
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.server.ApacheServer
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.TimeUnit.MILLISECONDS

class Java8HttpClientTest : HttpClientContract(::ApacheServer, Java8HttpClient(),
    ApacheClient(HttpClients.custom()
        .setDefaultRequestConfig(RequestConfig.custom().setResponseTimeout(100, MILLISECONDS).build()).build()
        , responseBodyMode = BodyMode.Stream)){

    @Test
    fun `allow configuring read timeout`(){
        val client = Java8HttpClient(readTimeout = Duration.ofMillis(10))
        val response = client(Request(Method.GET, "http://localhost:$port/delay").query("millis", "50"))
        assertThat(response.status, equalTo(CLIENT_TIMEOUT))
    }

    @Test
    fun `allow configuring connect timeout`(){
        val client = Java8HttpClient(connectionTimeout = Duration.ofMillis(10))
        val response = client(Request(Method.GET, "http://10.0.0.0:81/does-not-exist")) //non-routable host/port
        assertThat(response.status, equalTo(CLIENT_TIMEOUT))
    }
}
