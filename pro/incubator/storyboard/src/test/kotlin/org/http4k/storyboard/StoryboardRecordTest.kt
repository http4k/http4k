/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.storyboard.render.flatten
import org.junit.jupiter.api.Test
import org.openqa.selenium.By

class StoryboardRecordTest {

    private val app: HttpHandler = { req ->
        Response(OK).body("<html><body><a id='go' href='/next'>${req.uri.path}</a></body></html>")
    }

    @Test
    fun `storyboard returns a Story populated by the block`() {
        val story = storyboard("login flow") {
            val driver = webDriver(app)
            driver.get("/")
            driver.capture("Landed", "fresh load")
            driver.findElement(By.id("go")).click()
        }

        assertThat(story.title, equalTo("login flow"))
        val frames = story.flatten().map { it.frame }.dropHttpFrames()
        assertThat(frames.size, equalTo(2))
        assertThat(frames.map { it.title }, equalTo(listOf("Landed", "click [By.id: go]")))
    }

    @Test
    fun `storyboard fills durationMs`() {
        val story = storyboard("noop") { /* nothing */ }

        assertThat(story.durationMs != null, equalTo(true))
    }
}
