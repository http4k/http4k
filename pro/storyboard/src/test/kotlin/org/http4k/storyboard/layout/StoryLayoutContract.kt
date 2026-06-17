/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.layout

import org.http4k.storyboard.Chapter
import org.http4k.storyboard.Story
import org.http4k.storyboard.StoryFrame
import org.http4k.storyboard.StoryFrame.Level.Context
import org.http4k.storyboard.StoryFrame.Level.Detail
import org.http4k.storyboard.StoryFrame.Level.Story
import org.http4k.storyboard.StoryLayout
import org.http4k.storyboard.util.gzipBase64Encode
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
abstract class StoryLayoutContract(private val layout: StoryLayout) {

    @Test
    fun `renders a representative story`(approver: Approver) {
        approver.assertApproved(layout.render(knownStory))
    }
}

private val homePage = "<html><head><title>Home</title></head><body><h1>welcome</h1></body></html>".gzipBase64Encode()
private val detailPage = "<html><head><title>Detail</title></head><body><p>detail content</p></body></html>".gzipBase64Encode()

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
                        StoryFrame(
                            title = "Intro splash",
                            notes = "kick-off note",
                            dom = "<h1>Demo</h1>".gzipBase64Encode(),
                            level = Context
                        ),
                        StoryFrame(
                            title = "Source",
                            notes = "snippet from prod",
                            dom = "<pre><code class=\"language-kotlin\">fun greet() = \"hi\"</code></pre>".gzipBase64Encode(),
                            level = Context
                        )
                    )
                ),
                Chapter(
                    title = "Run",
                    frames = listOf(
                        StoryFrame(
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
                                StoryFrame(
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
