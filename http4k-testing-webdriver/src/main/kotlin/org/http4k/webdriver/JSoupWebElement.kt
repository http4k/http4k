package org.http4k.webdriver

import org.jsoup.nodes.Element
import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.OutputType
import org.openqa.selenium.Point
import org.openqa.selenium.Rectangle
import org.openqa.selenium.WebElement

data class JSoupWebElement(private val element: Element) : WebElement {
    override fun findElement(by: By): WebElement? = JSoupElementFinder(element).findElement(by)

    override fun getTagName(): String = element.tagName()

    override fun getText(): String = element.text()

    override fun getAttribute(name: String): String? = element.attr(name)

    override fun findElements(by: By) = JSoupElementFinder(element).findElements(by)

    override fun isDisplayed(): Boolean = throw FeatureNotImplementedYet()

    override fun clear(): Unit = throw FeatureNotImplementedYet()

    override fun submit(): Unit = throw FeatureNotImplementedYet()

    override fun getLocation(): Point = throw FeatureNotImplementedYet()

    override fun <X : Any?> getScreenshotAs(target: OutputType<X>?): X = throw FeatureNotImplementedYet()

    override fun click(): Unit = throw FeatureNotImplementedYet()

    override fun getSize(): Dimension = throw FeatureNotImplementedYet()

    override fun isSelected(): Boolean = throw FeatureNotImplementedYet()

    override fun isEnabled(): Boolean = throw FeatureNotImplementedYet()

    override fun sendKeys(vararg keysToSend: CharSequence?): Unit = throw FeatureNotImplementedYet()

    override fun getRect(): Rectangle = throw FeatureNotImplementedYet()

    override fun getCssValue(propertyName: String?): String = throw FeatureNotImplementedYet()
}