package org.http4k.ai.util

import com.squareup.moshi.JsonAdapter
import se.ansman.kotshi.KotshiJsonAdapterFactory

@KotshiJsonAdapterFactory
object Http4kAiCoreJsonAdapterFactory : JsonAdapter.Factory by KotshiHttp4kAiCoreJsonAdapterFactory
