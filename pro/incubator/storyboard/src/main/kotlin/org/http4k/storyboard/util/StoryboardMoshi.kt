/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.util

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import org.http4k.storyboard.StoryFrame
import java.lang.reflect.Type

object StoryboardMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .addLast(ListAdapter)
        .addLast(StoryFrameAdapter)
        .asConfigurable()
        .withStandardMappings()
        .done()
)

object StoryFrameAdapter : JsonAdapter.Factory {
    override fun create(p0: Type, p1: MutableSet<out Annotation>, p2: Moshi) =
        if (p0.typeName == StoryFrame::class.java.typeName) p2.adapter(Any::class.java) else null
}
