/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.storyboard.junit.RenderStoryboard
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(RenderStoryboard::class)
class StoryboardTest {

    private val handler: HttpHandler = { req ->
        Response(OK).body("<html><head><title>${req.uri.path}</title></head><body>page</body></html>")
    }

    @Test
    fun `records frames`(storyboard: Storyboard) {
        val driver = storyboard.webDriver(handler)
        driver.get("/1")
        driver.capture("Click1", "notes1")
        driver.get("/2")
        driver.capture("Click2", "notes2")
        driver.get("/3")
        driver.capture("Click3", "notes3")
        driver.get("/4")
        driver.capture("Click4", "notes4")
        driver.get("/5")
        driver.capture("Click5", "notes5")
    }

    @Test
    fun `chapters group the journey into named sections`(storyboard: Storyboard) {
        val driver = storyboard.webDriver(handler)

        storyboard {
            chapter("Login") {
                driver.get("/login")
                driver.capture("Login page", "empty form")
                driver.get("/dashboard")
                driver.capture("Logged in", "post-login dashboard")
            }

            chapter("Browse") {
                driver.get("/catalog")
                driver.capture("Catalog", "list of items")
                driver.get("/catalog/item-42")
                driver.capture("Item detail", "selected item-42")
            }

            chapter("Checkout") {
                driver.get("/checkout")
                driver.capture("Cart", "review before paying")

                chapter("Confirm") {
                    driver.get("/checkout/confirm")
                    driver.capture("Confirmation", "final review")
                    driver.get("/checkout/done")
                    driver.capture("Receipt", "post-payment success")
                }
            }
        }
    }
}
