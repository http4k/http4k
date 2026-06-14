/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import org.http4k.base64Encode
import org.http4k.storyboard.CaptureMode.Mixed
import org.http4k.storyboard.StoryFrame.Kind.Auto
import org.http4k.storyboard.StoryFrame.Kind.Manual
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement

enum class CaptureMode { Mixed, ManualOnly }

class RecordingWebDriver(
    private val delegate: WebDriver,
    private val captureMode: CaptureMode = Mixed
) : WebDriver by delegate {

    private val recorded = mutableListOf<StoryFrame>()

    fun capture(title: String, notes: String = "") {
        recorded += StoryFrame(title, notes, snapshot(), Manual)
    }

    fun frames(): List<StoryFrame> = recorded.toList()

    fun manualFrames(): List<StoryFrame> = recorded.filter { it.kind == Manual }

    override fun findElement(by: By): WebElement =
        RecordingWebElement(delegate.findElement(by), ::autoCapture, by.toString())

    override fun findElements(by: By): List<WebElement> =
        delegate.findElements(by).mapIndexed { i, e ->
            RecordingWebElement(e, ::autoCapture, "$by[$i]")
        }

    private fun autoCapture(title: String) {
        if (captureMode == Mixed) recorded += StoryFrame(title, "", snapshot(), Auto)
    }

    private fun snapshot(): String = (delegate.pageSource ?: "").base64Encode()
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
        RecordingWebElement(delegate.findElement(by), capture, "$description -> $by")

    override fun findElements(by: By): List<WebElement> =
        delegate.findElements(by).mapIndexed { i, e ->
            RecordingWebElement(e, capture, "$description -> $by[$i]")
        }
}
