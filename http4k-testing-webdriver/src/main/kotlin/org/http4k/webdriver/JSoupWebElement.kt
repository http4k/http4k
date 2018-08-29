package org.http4k.webdriver

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.FormField
import org.http4k.lens.Validator
import org.http4k.lens.WebForm
import org.http4k.lens.webForm
import org.jsoup.nodes.Element
import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.OutputType
import org.openqa.selenium.Point
import org.openqa.selenium.Rectangle
import org.openqa.selenium.WebElement

data class JSoupWebElement(private val navigate: Navigate, private val getURL: GetURL, private val element: Element) : WebElement {

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
            val inputs = it
                .findElements(By.tagName("input"))
                .filter { it.getAttribute("name") != "" }
                .map { it.getAttribute("name") to listOf(it.getAttribute("value")) }
            val textareas = it.findElements(By.tagName("textarea"))
                .filter { it.getAttribute("name") != "" }
                .map { it.getAttribute("name") to listOf(it.text) }
            val selects = it.findElements(By.tagName("select"))
                .filter { it.getAttribute("name") != "" }
                .map {
                    it.getAttribute("name") to it.findElements(By.tagName("option"))
                        .filter { it.isSelected }
                        .map { it.getAttribute("value") }
                }

            val form = WebForm(inputs.plus(textareas).plus(selects)
                .groupBy { it.first }
                .mapValues { it.value.map { it.second }.flatMap { it } })

            val body = Body.webForm(Validator.Strict,
                *(form.fields.map { FormField.multi.required(it.key) }.toTypedArray())).toLens()

            val uri = (it.element.attr("action") ?: getURL()) ?: "<unknown>"
            val postRequest = Request(method, uri).with(body of form)

            if (method == Method.POST) navigate(postRequest)
            else navigate(Request(method, Uri.of(uri).query(postRequest.bodyString())).body(""))
        }
    }

    override fun getLocation(): Point = throw FeatureNotImplementedYet

    override fun <X : Any?> getScreenshotAs(target: OutputType<X>?): X = throw FeatureNotImplementedYet

    override fun click() {
        if (isA("a")) {
            element.attr("href")?.let { navigate(Request(Method.GET, it)) }
        } else if (isCheckable()) {
            if (isSelected) clear()
            else element.attr("checked", "checked")
        } else if (isA("option")) {
            val currentSelectIsMultiple = current("select")?.element?.hasAttr("multiple") == true

            val oldValue = isSelected

            if (currentSelectIsMultiple) element.attr("selected", "selected")
            else current("select")?.findElements(By.tagName("option"))?.forEach { it.clear() }

            if (oldValue && !currentSelectIsMultiple) clear()
            else element.attr("selected", "selected")
        } else if (isA("button") && element.attr("type")?:"submit".toLowerCase() == "submit") {
            submit()
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

    override fun findElement(by: By): WebElement? = JSoupElementFinder(navigate, getURL, element).findElement(by)

    override fun findElements(by: By) = JSoupElementFinder(navigate, getURL, element).findElements(by)

    private fun current(tag: String): JSoupWebElement? = if (isA(tag)) this else this.parent()?.current(tag)

    private fun parent(): JSoupWebElement? = element.parent()?.let { JSoupWebElement(navigate, getURL, it) }

    private fun isA(tag: String) = tagName.toLowerCase() == tag.toLowerCase()
}