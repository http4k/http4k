/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import org.http4k.storyboard.Story.Outcome.Passed
import org.http4k.storyboard.junit.RenderStoryboard
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.openqa.selenium.By
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@ExtendWith(ApprovalTest::class)
abstract class EndToEndContract(private val renderer: StoryRenderer) {

    @JvmField
    @RegisterExtension
    val storyboard = RenderStoryboard(
        renderer = renderer,
        clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
    )

    @Test
    fun `renders an end-to-end recording with every frame type`(approver: Approver, storyboard: Storyboard) {

        val story = storyboard {
            val driver = StoryboardWebDriver(threeStageApp(otel), this)

            image("Sample image", File("src/test/resources/org/http4k/storyboard/sample.png"))

            chapter("Setup") {
                html("Splash", "<h1>End-to-end demo</h1><p>Walks every frame type.</p>")
                code("Sample source", File("src/test/resources/org/http4k/storyboard/sample.kt"))
            }

            driver.chapter("Run") {
                driver.get("http://localhost/home")
                driver.capture("Home", "first navigation")

                driver.chapter("Detail") {
                    driver.findElement(By.id("next")).click()
                    driver.capture("Detail page", "after the click")
                }
            }

            chapter("Wrap-up") {
                html("Outro", "<p>Thanks for watching.</p>")
            }
        }.toStory(Passed, defaultExtractors)

        approver.assertApproved(renderer.render(story))
    }
}
