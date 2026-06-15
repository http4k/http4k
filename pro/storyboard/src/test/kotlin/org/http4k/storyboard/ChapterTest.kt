/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class ChapterTest {

    @Test
    fun `sibling chapters appear under the root recording chapter`() {
        val story = recordStory { driver ->
            chapter("Login") { driver.capture("login page") }
            chapter("Checkout") { driver.capture("checkout page") }
        }

        val root = story.chapters.single()
        assertThat(root.title, equalTo("test"))
        assertThat(root.frames, equalTo(emptyList()))
        assertThat(root.children.map { it.title }, equalTo(listOf("Login", "Checkout")))
        assertThat(root.children[0].frames.map { it.title }, equalTo(listOf("login page")))
        assertThat(root.children[1].frames.map { it.title }, equalTo(listOf("checkout page")))
    }

    @Test
    fun `nested chapters produce a nested chapter tree`() {
        val story = recordStory { driver ->
            chapter("Outer") {
                chapter("Inner") {
                    driver.capture("deep")
                }
            }
        }

        val outer = story.chapters.single().children.single()
        assertThat(outer.title, equalTo("Outer"))
        val inner = outer.children.single()
        assertThat(inner.title, equalTo("Inner"))
        assertThat(inner.frames.map { it.title }, equalTo(listOf("deep")))
    }

    @Test
    fun `frames captured outside any user chapter attach to the recording root`() {
        val story = recordStory { driver ->
            driver.capture("before")
            chapter("Inside") { driver.capture("during") }
            driver.capture("after")
        }

        val root = story.chapters.single()
        assertThat(root.title, equalTo("test"))
        assertThat(root.frames.map { it.title }, equalTo(listOf("before", "after")))
        assertThat(root.children.single().frames.map { it.title }, equalTo(listOf("during")))
    }
}
