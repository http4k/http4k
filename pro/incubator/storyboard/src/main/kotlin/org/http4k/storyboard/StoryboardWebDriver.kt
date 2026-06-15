/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import org.http4k.base64Encode
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.storyboard.StoryFrame.Level
import org.http4k.storyboard.StoryFrame.Level.Detail
import org.http4k.storyboard.StoryFrame.Level.Story
import org.http4k.storyboard.frame.WebDriverCapture
import org.http4k.webdriver.Http4kWebDriver
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.time.Clock

class StoryboardWebDriver(
    private val delegate: WebDriver,
    val storyboard: Storyboard
) : WebDriver by delegate {

    fun capture(title: String, notes: String = "", level: Level = Story) {
        storyboard.captureFrame(WebDriverCapture(title, notes, pageSource?.base64Encode() ?: "", level))
    }

    override fun findElement(by: By): WebElement =
        StoryboardWebElement(delegate.findElement(by), ::recordInteraction, by.toString())

    override fun findElements(by: By): List<WebElement> =
        delegate.findElements(by).mapIndexed { i, e ->
            StoryboardWebElement(e, ::recordInteraction, "$by[$i]")
        }

    private fun recordInteraction(title: String) {
        storyboard.captureFrame(WebDriverCapture(title, "", pageSource?.base64Encode() ?: "", Detail))
    }
}

fun StoryboardWebDriver(
    http: HttpHandler,
    storyboard: Storyboard,
    clock: Clock = Clock.systemUTC()
): StoryboardWebDriver = StoryboardWebDriver(
    Http4kWebDriver(ClientFilters.OpenTelemetryTracing(storyboard.otel).then(http), clock),
    storyboard
)

class StoryboardWebElement(
    private val delegate: WebElement,
    private val capture: (String) -> Unit,
    private val description: String
) : WebElement by delegate {

    override fun click() {
        delegate.click()
        capture("click [$description]")
    }

    override fun submit() {
        delegate.submit()
        capture("submit [$description]")
    }

    override fun clear() {
        delegate.clear()
        capture("clear [$description]")
    }

    override fun sendKeys(vararg keysToSend: CharSequence) {
        delegate.sendKeys(*keysToSend)
        capture("sendKeys '${keysToSend.joinToString("")}' [$description]")
    }

    override fun findElement(by: By): WebElement =
        StoryboardWebElement(delegate.findElement(by), capture, "$description -> $by")

    override fun findElements(by: By): List<WebElement> =
        delegate.findElements(by).mapIndexed { i, e ->
            StoryboardWebElement(e, capture, "$description -> $by[$i]")
        }
}
