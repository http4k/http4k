/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.renderer

import org.http4k.base64Encode
import org.http4k.storyboard.Chapter
import org.http4k.storyboard.Story
import org.http4k.storyboard.StoryFrame.Level.Context
import org.http4k.storyboard.StoryFrame.Level.Detail
import org.http4k.storyboard.StoryFrame.Level.Story
import org.http4k.storyboard.StoryRenderer
import org.http4k.storyboard.frame.Code
import org.http4k.storyboard.frame.Html
import org.http4k.storyboard.frame.WebDriverCapture
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
abstract class StoryRendererContract(private val renderer: StoryRenderer) {

    @Test
    fun `renders a representative story`(approver: Approver) {
        approver.assertApproved(renderer.render(knownStory))
    }
}

private val homePage = "<html><head><title>Home</title></head><body><h1>welcome</h1></body></html>".base64Encode()
private val detailPage = "<html><head><title>Detail</title></head><body><p>detail content</p></body></html>".base64Encode()

internal val knownStory = Story(
    title = "demo recording",
    series = "ExampleSeries",
    chapters = listOf(
        Chapter(
            title = "demo recording",
            children = listOf(
                Chapter(
                    title = "Setup",
                    frames = listOf(
                        Html(
                            title = "Intro splash",
                            notes = "kick-off note",
                            dom = "<h1>Demo</h1>".base64Encode(),
                            level = Context
                        ),
                        Code(
                            title = "Source",
                            notes = "snippet from prod",
                            dom = "<pre><code class=\"language-kotlin\">fun greet() = \"hi\"</code></pre>".base64Encode(),
                            language = "kotlin",
                            source = "fun greet() = \"hi\"",
                            level = Context
                        )
                    )
                ),
                Chapter(
                    title = "Run",
                    frames = listOf(
                        WebDriverCapture(
                            title = "Home",
                            notes = "first load",
                            dom = homePage,
                            level = Story
                        )
                    ),
                    children = listOf(
                        Chapter(
                            title = "Detail",
                            frames = listOf(
                                WebDriverCapture(
                                    title = "After click",
                                    notes = "post-nav",
                                    dom = detailPage,
                                    level = Detail
                                )
                            )
                        )
                    )
                )
            )
        )
    )
)
