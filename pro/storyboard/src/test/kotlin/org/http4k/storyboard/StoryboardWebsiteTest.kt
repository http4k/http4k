/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import org.http4k.client.JavaHttpClient
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.storyboard.frame.webDriver
import org.http4k.storyboard.junit.RenderStoryboard
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(RenderStoryboard::class)
class StoryboardWebsiteTest {

    @Test
    fun `records website`(storyboard: Storyboard) {
        val driver = storyboard.webDriver(
            ClientFilters.SetHostFrom(Uri.of("https://www.http4k.org"))
                .then(JavaHttpClient())
        )
        driver.get("/")
        driver.capture("Home")
        driver.get("/pro")
        driver.capture("Pro")
    }
}
