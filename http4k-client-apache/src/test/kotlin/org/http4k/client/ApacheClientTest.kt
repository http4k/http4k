package org.http4k.client

import org.apache.http.config.SocketConfig
import org.apache.http.impl.client.HttpClients
import org.http4k.server.SunHttp

class ApacheClientTest : Http4kClientContract({ SunHttp(it) }, ApacheClient(),
    ApacheClient(HttpClients.custom()
        .setDefaultSocketConfig(
            SocketConfig.custom()
                .setSoTimeout(100)
                .build()
        ).build()
        , bodyMode = ResponseBodyMode.Stream))
