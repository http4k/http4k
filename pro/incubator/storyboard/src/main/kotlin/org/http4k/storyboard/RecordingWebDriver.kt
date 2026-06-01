/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import org.http4k.base64Encode
import org.http4k.webdriver.Http4kWebDriver
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement

class RecordingWebDriver(private val delegate: Http4kWebDriver) : WebDriver by delegate {

    private val recorded = mutableListOf<StoryFrame>()

    fun capture(title: String, notes: String = "") {
        recorded += StoryFrame(title, notes, (delegate.pageSource ?: "").base64Encode())
    }

    fun frames(): List<StoryFrame> = recorded.toList()

    override fun findElement(by: By): WebElement =
        RecordingWebElement(delegate.findElement(by), ::capture, by.toString())

    override fun findElements(by: By): List<WebElement> =
        delegate.findElements(by).mapIndexed { i, e ->
            RecordingWebElement(e, ::capture, "$by[$i]")
        }
}

internal class RecordingWebElement(
    private val delegate: WebElement,
    private val capture: (String) -> Unit,
    private val description: String
) : WebElement by delegate {

    override fun click() {
        delegate.click()
        capture("click [$description]")
    }

    override fun findElement(by: By): WebElement =
        RecordingWebElement(delegate.findElement(by), capture, "$description -> $by")

    override fun findElements(by: By): List<WebElement> =
        delegate.findElements(by).mapIndexed { i, e ->
            RecordingWebElement(e, capture, "$description -> $by[$i]")
        }
}
