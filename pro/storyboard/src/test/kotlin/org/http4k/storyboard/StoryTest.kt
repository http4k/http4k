/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.storyboard.StoryFrame.Level.Story
import org.http4k.storyboard.util.StoryboardMoshi
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class StoryTest {

    @Test
    fun `empty story`(approver: Approver) {
        approver(Story("demo"))
    }

    @Test
    fun `story with nested chapters`(approver: Approver) {
        approver(
            Story(
                title = "demo",
                chapters = listOf(
                    Chapter(
                        title = "demo",
                        frames = listOf(StoryFrame("Home", "first load", "PGh0bWw+PC9odG1sPg==", Story)),
                        children = listOf(
                            Chapter(
                                title = "Login",
                                frames = listOf(StoryFrame("Logged in", "", "PGRpdj5pbjwvZGl2Pg==", Story))
                            )
                        )
                    )
                )
            )
        )
    }

    private operator fun Approver.invoke(story: Story) = assertApproved(
        Response(OK).with(CONTENT_TYPE of APPLICATION_JSON).body(StoryboardMoshi.asFormatString(story))
    )
}
