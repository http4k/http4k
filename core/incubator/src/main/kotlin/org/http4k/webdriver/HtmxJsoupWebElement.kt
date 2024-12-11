package org.http4k.webdriver

import org.http4k.core.HttpHandler
import org.openqa.selenium.By
import org.openqa.selenium.OutputType
import org.openqa.selenium.WebElement

data class HtmxJsoupWebElement(val delegate: JSoupWebElement, val handler: HttpHandler) : WebElement by delegate {
    private fun toHtmx(element: WebElement): HtmxJsoupWebElement =
        HtmxJsoupWebElement(element as JSoupWebElement, handler)

    override fun findElements(by: By): List<WebElement> =
        delegate
            .findElements(by)
            .map { toHtmx(it) }

    override fun findElement(by: By): WebElement =
        delegate
            .findElement(by)
            .let(::toHtmx)

    override fun <X : Any> getScreenshotAs(target: OutputType<X>): X = delegate.getScreenshotAs(target)

    override fun click() {
        delegate.click()
        HtmxCommand.from(this)?.performOn(this)
    }

    override fun submit() {
        val hxCommand = HtmxCommand.from(this)
        when {
            hxCommand != null -> hxCommand.performOn(this)
            else -> delegate.submit()
        }
    }

    override fun sendKeys(vararg keysToSend: CharSequence) {
        delegate.sendKeys(*keysToSend)
        HtmxCommand.from(this)?.performOn(this)
    }

    override fun toString(): String = delegate.toString()
}
