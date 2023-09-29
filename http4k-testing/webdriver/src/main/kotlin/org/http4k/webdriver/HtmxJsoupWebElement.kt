package org.http4k.webdriver

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.OutputType
import org.openqa.selenium.Point
import org.openqa.selenium.Rectangle
import org.openqa.selenium.WebElement

data class HtmxJsoupWebElement(val delegate: JSoupWebElement, val handler: HttpHandler) : WebElement {
    private fun toHtmx(element: WebElement): HtmxJsoupWebElement =
        when (element) {
            is JSoupWebElement -> HtmxJsoupWebElement(element, handler)
            else -> throw RuntimeException("could not convert $element to HtmxJsoupWebElement")
        }

    override fun findElements(by: By): List<WebElement> =
        delegate
            .findElements(by)
            .map { toHtmx(it) }

    override fun findElement(by: By): WebElement? =
        delegate
            .findElement(by)
            ?.let { toHtmx(it) }

    override fun <X : Any?> getScreenshotAs(target: OutputType<X>?): X = delegate.getScreenshotAs(target)

    private fun hxCommand(): String? = getAttribute("hx-get")

    private fun performHxCommand(command: String) {
        val response = handler(Request(Method.GET, command))
        this.delegate.element.childNodes().forEach { it.remove() }
        this.delegate.element.appendChild(Jsoup.parse(response.bodyString()))
    }

    override fun click() {
        val hxCommand = hxCommand()
        when {
            hxCommand != null -> performHxCommand(hxCommand)
            else -> delegate.click()
        }
    }

    override fun submit() {
        delegate.submit()
    }

    override fun sendKeys(vararg keysToSend: CharSequence) =
        delegate.sendKeys(*keysToSend)

    override fun clear() = delegate.clear()

    override fun getTagName(): String = delegate.getTagName()

    override fun getAttribute(name: String): String? = delegate.getAttribute(name)

    override fun isSelected(): Boolean = delegate.isSelected()

    override fun isEnabled(): Boolean = delegate.isEnabled()

    override fun getText(): String = delegate.getText()

    override fun isDisplayed(): Boolean = delegate.isDisplayed()

    override fun getLocation(): Point = delegate.getLocation()

    override fun getSize(): Dimension = delegate.getSize()

    override fun getRect(): Rectangle = delegate.getRect()

    override fun getCssValue(propertyName: String): String = delegate.getCssValue(propertyName)
}
