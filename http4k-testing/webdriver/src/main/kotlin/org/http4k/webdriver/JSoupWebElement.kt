package org.http4k.webdriver

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Method.*
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.FormField
import org.http4k.lens.Validator
import org.http4k.lens.WebForm
import org.http4k.lens.webForm
import org.jsoup.nodes.Element
import org.openqa.selenium.Dimension
import org.openqa.selenium.OutputType
import org.openqa.selenium.Point
import org.openqa.selenium.Rectangle
import org.openqa.selenium.WebElement

data class JSoupWebElement(private val navigate: Navigate, private val getURL: GetURL, val element: Element) : WebElement {

    override fun toString(): String = element.toString()

    override fun getTagName(): String = element.tagName()

    override fun getText(): String = element.text()

    override fun getAttribute(name: String): String? {
        return when {
            booleanAttributes.contains(name) && element.hasAttr(name) -> "true"
            booleanAttributes.contains(name) && !element.hasAttr(name) -> null
            else -> element.attr(name)
        }
    }

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
            val method = it.element.attr("method")?.let(String::toUpperCase)?.let(Method::valueOf) ?: POST
            val inputs = it
                .findElements(By.tagName("input"))
                .filter { it.getAttribute("name") != "" }
                .filterNot(::isUncheckedInput)
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
            val buttons = it.findElements(By.tagName("button"))
                .filter { it.getAttribute("name") != "" && it == this }
                .map { it.getAttribute("name") to listOf(it.getAttribute("value")) }
            val form = WebForm(inputs.plus(textareas).plus(selects).plus(buttons)
                .groupBy { it.first }
                .mapValues { it.value.map { it.second }.flatten() })

            val body = Body.webForm(Validator.Strict,
                *(form.fields.map { FormField.multi.required(it.key) }.toTypedArray())).toLens()

            val formtarget = Uri.of(it.element.attr("action") ?: "")
            val current = getURL()
            val action = when {
                formtarget.host == "" && formtarget.path == "" && current != null -> Uri.of(current)
                formtarget.host == "" && current != null -> Uri.of(current).path(formtarget.path)
                else -> formtarget
            }
            val postRequest = Request(method, action.toString()).with(body of form)

            if (method == POST) navigate(postRequest)
            else navigate(Request(method, action.query(postRequest.bodyString())).body(""))
        }
    }

    private fun isUncheckedInput(input: WebElement): Boolean =
        (listOf("checkbox", "radio").contains(input.getAttribute("type"))) && input.getAttribute("checked") == null

    override fun getLocation(): Point = throw FeatureNotImplementedYet

    override fun <X : Any?> getScreenshotAs(target: OutputType<X>?): X = throw FeatureNotImplementedYet

    override fun click() {
        if (isA("a")) {
            element.attr("href")?.let { navigate(Request(GET, it)) }
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
        } else if (isA("button")) {
            val t = element.attr("type")
            if (t == "" || t.toLowerCase() == "submit")
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

    override fun equals(other: Any?) = (other as? JSoupWebElement)?.element?.hasSameValue(element) ?: false

    override fun getRect(): Rectangle = throw FeatureNotImplementedYet

    override fun getCssValue(propertyName: String?): String = throw FeatureNotImplementedYet

    override fun hashCode(): Int = element.hashCode()

    override fun findElement(by: org.openqa.selenium.By): WebElement? = JSoupElementFinder(navigate, getURL, element).findElement(by)

    override fun findElements(by: org.openqa.selenium.By) = JSoupElementFinder(navigate, getURL, element).findElements(by)

    private fun current(tag: String): JSoupWebElement? = if (isA(tag)) this else parent()?.current(tag)

    private fun parent(): JSoupWebElement? = element.parent()?.let { JSoupWebElement(navigate, getURL, it) }

    private fun isA(tag: String) = tagName.toLowerCase() == tag.toLowerCase()

    companion object {
        private val booleanAttributes = listOf(
            "async",
            "autofocus",
            "autoplay",
            "checked",
            "compact",
            "complete",
            "controls",
            "declare",
            "defaultchecked",
            "defaultselected",
            "defer",
            "disabled",
            "draggable",
            "ended",
            "formnovalidate",
            "hidden",
            "indeterminate",
            "iscontenteditable",
            "ismap",
            "itemscope",
            "loop",
            "multiple",
            "muted",
            "nohref",
            "noresize",
            "noshade",
            "novalidate",
            "nowrap",
            "open",
            "paused",
            "pubdate",
            "readonly",
            "required",
            "reversed",
            "scoped",
            "seamless",
            "seeking",
            "selected",
            "truespeed",
            "willvalidate"
        )
    }
}
