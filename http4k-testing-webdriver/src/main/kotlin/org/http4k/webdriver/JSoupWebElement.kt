package org.http4k.webdriver

import org.http4k.core.Method
import org.jsoup.nodes.Element
import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.OutputType
import org.openqa.selenium.Point
import org.openqa.selenium.Rectangle
import org.openqa.selenium.WebElement

class JSoupWebElement(private val navigate: Navigate, protected val element: Element) : WebElement {

    override fun getTagName(): String = element.tagName()

    override fun getText(): String = element.text()

    override fun getAttribute(name: String): String? = element.attr(name)

    override fun isDisplayed(): Boolean = throw FeatureNotImplementedYet

    override fun clear() = throw FeatureNotImplementedYet

    override fun submit() {
        currentForm()?.let {
            val method = it.getAttribute("method")?.let(String::toUpperCase)?.let(Method::valueOf) ?: Method.POST
            navigate(method, it.getAttribute("action") ?: "<unknown>")
        }
    }

    override fun getLocation(): Point = throw FeatureNotImplementedYet

    override fun <X : Any?> getScreenshotAs(target: OutputType<X>?): X = throw FeatureNotImplementedYet

    override fun click() {
        getAttribute("href")?.let { if (isA("a")) navigate(Method.GET, it) }
    }

    override fun getSize(): Dimension = throw FeatureNotImplementedYet

    override fun isSelected(): Boolean = throw FeatureNotImplementedYet

    override fun isEnabled(): Boolean = !element.hasAttr("disabled")

    override fun sendKeys(vararg keysToSend: CharSequence?) = throw FeatureNotImplementedYet

    override fun getRect(): Rectangle = throw FeatureNotImplementedYet

    override fun getCssValue(propertyName: String?): String = throw FeatureNotImplementedYet

    override fun equals(other: Any?): Boolean = element == element

    override fun hashCode(): Int = element.hashCode()

    override fun findElement(by: By): WebElement? = JSoupElementFinder(navigate, element).findElement(by)

    override fun findElements(by: By) = JSoupElementFinder(navigate, element).findElements(by)

    private fun currentForm(): JSoupWebElement? = if (isA("form")) this else this.parent()?.currentForm()

    private fun parent(): JSoupWebElement? = element.parent()?.let { JSoupWebElement(navigate, it) }

    private fun isA(tag: String) = tagName.toLowerCase() == tag
}