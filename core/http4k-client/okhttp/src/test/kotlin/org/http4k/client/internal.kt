package org.http4k.client

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

internal val timeout = OkHttpClient.Builder()
    .connectTimeout(100, TimeUnit.MILLISECONDS)
    .readTimeout(100, TimeUnit.MILLISECONDS)
    .writeTimeout(100, TimeUnit.MILLISECONDS)
    .build()
