package org.http4k.webdriver

import org.http4k.core.Method
import org.jsoup.nodes.Element
import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.OutputType
import org.openqa.selenium.Point
import org.openqa.selenium.Rectangle
import org.openqa.selenium.WebElement

data class JSoupWebElement(private val navigate: Navigate, private val element: Element) : WebElement {

    override fun getTagName(): String = element.tagName()

    override fun getText(): String = element.text()

    override fun getAttribute(name: String): String? = element.attr(name)

    override fun isDisplayed(): Boolean = throw FeatureNotImplementedYet

    override fun clear() {
        if (isA("option")) {
            element.removeAttr("selected")
        } else if (isCheckable()) {
            element.removeAttr("checked")
        }
    }

    override fun submit() {
        current("form")?.let {
            val method = it.element.attr("method")?.let(String::toUpperCase)?.let(Method::valueOf) ?: Method.POST
            navigate(method, it.element.attr("action") ?: "<unknown>")
        }
    }

    override fun getLocation(): Point = throw FeatureNotImplementedYet

    override fun <X : Any?> getScreenshotAs(target: OutputType<X>?): X = throw FeatureNotImplementedYet

    override fun click() {
        if (isA("a")) {
            element.attr("href")?.let { navigate(Method.GET, it) }
        } else if (isCheckable()) {
            element.attr("checked", "checked")
        } else if (isA("option")) {
            val currentSelectIsMultiple = current("select")?.element?.hasAttr("multiple") ?: false

            val oldValue = isSelected

            if (currentSelectIsMultiple) element.attr("selected", "selected")
            else current("select")?.findElements(By.tagName("option"))?.forEach { it.clear() }

            if (oldValue && !currentSelectIsMultiple) clear()
            else element.attr("selected", "selected")
        }
    }

    private fun isCheckable() = isA("input") && setOf("checkbox", "radio").contains(element.attr("type"))

    override fun getSize(): Dimension = throw FeatureNotImplementedYet

    override fun isSelected(): Boolean = when {
        isA("option") -> element.hasAttr("selected")
        isCheckable() -> element.hasAttr("checked")
        else -> false
    }

    override fun isEnabled(): Boolean = !element.hasAttr("disabled")

    override fun sendKeys(vararg keysToSend: CharSequence) {
        val valueToSet = keysToSend.joinToString("")
        if (isA("textarea")) {
            element.text(valueToSet)
        } else if (isA("input")) {
            element.attr("value", valueToSet)
        }
    }

    override fun getRect(): Rectangle = throw FeatureNotImplementedYet

    override fun getCssValue(propertyName: String?): String = throw FeatureNotImplementedYet

    override fun equals(other: Any?): Boolean = element == element

    override fun hashCode(): Int = element.hashCode()

    override fun findElement(by: By): WebElement? = JSoupElementFinder(navigate, element).findElement(by)

    override fun findElements(by: By) = JSoupElementFinder(navigate, element).findElements(by)

    private fun current(tag: String): JSoupWebElement? = if (isA(tag)) this else this.parent()?.current(tag)

    private fun parent(): JSoupWebElement? = element.parent()?.let { JSoupWebElement(navigate, it) }

    private fun isA(tag: String) = tagName.toLowerCase() == tag.toLowerCase()
}