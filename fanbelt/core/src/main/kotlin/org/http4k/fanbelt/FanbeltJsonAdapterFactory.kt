package org.http4k.fanbelt

import com.squareup.moshi.JsonAdapter
import se.ansman.kotshi.KotshiJsonAdapterFactory

@KotshiJsonAdapterFactory
internal object FanbeltJsonAdapterFactory : JsonAdapter.Factory by KotshiFanbeltJsonAdapterFactory
