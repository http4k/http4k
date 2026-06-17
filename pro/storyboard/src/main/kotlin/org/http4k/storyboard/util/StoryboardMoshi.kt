/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.util

import com.squareup.moshi.Moshi
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings

object StoryboardMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .addLast(ListAdapter)
        .asConfigurable()
        .withStandardMappings()
        .done()
)
