package org.http4k.webdriver

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.relative
import org.http4k.core.with
import org.http4k.lens.FormField
import org.http4k.lens.MultipartForm
import org.http4k.lens.MultipartFormField
import org.http4k.lens.MultipartFormFile
import org.http4k.lens.Validator
import org.http4k.lens.WebForm
import org.http4k.lens.multipartForm
import org.http4k.lens.webForm
import org.jsoup.nodes.Element
import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.OutputType
import org.openqa.selenium.Point
import org.openqa.selenium.Rectangle
import org.openqa.selenium.WebElement
import java.io.File
import java.nio.file.Files
import java.util.Locale.getDefault

data class JSoupWebElement(private val navigate: Navigate, private val getURL: GetURL, val element: Element) :
    WebElement {

    override fun toString(): String = element.toString()

    override fun getTagName(): String = element.tagName()

    override fun getText(): String = element.text()

    @Deprecated("Deprecated in Java")
    override fun getAttribute(name: String) = getDomAttribute(name)

    override fun getDomAttribute(name: String): String? {
        return when {
            booleanAttributes.contains(name) && element.hasAttr(name) -> "true"
            booleanAttributes.contains(name) && !element.hasAttr(name) -> null
            else -> element.attr(name)
        }
    }

    override fun getDomProperty(name: String): String? {
        // TODO need to work out how to get the property from the element...
        throw UnsupportedOperationException("Not implemented")
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
        associatedForm()?.let { form ->
            val enctype = form.getDomAttribute("enctype") ?: ContentType.APPLICATION_FORM_URLENCODED.value

            val method =
                runCatching { Method.valueOf(form.element.attr("method").uppercase(getDefault())) }.getOrDefault(POST)

            val inputs = associatedFormElements(form, "input")
                .asSequence()
                .filter { it.getDomAttribute("name") != "" }
                .filter { it.isNotDisabled() }
                .filterNot { it.isAFileInput() }
                .filterNot { it.isAnInactiveSubmitInput() }
                .filterNot(::isUncheckedInput)
                .map { it.getDomAttribute("name") to listOf(it.getDomAttribute("value")) }
                .toList()

            val fileInputs = associatedFormElements(form, "input")
                .filter { it.getDomAttribute("name") != "" }
                .filter { it.isNotDisabled() }
                .filter { it.isAFileInput() }
                .map { it.getDomAttribute("name") to listOf(it.getDomAttribute("value")) }

            val textAreas = associatedFormElements(form, "textarea")
                .filter { it.isNotDisabled() }
                .filter { it.getDomAttribute("name") != "" }
                .map { it.getDomAttribute("name") to listOf(it.text) }

            val selects = associatedFormElements(form, "select")
                .filter { it.isNotDisabled() }
                .filter { it.getDomAttribute("name") != "" }
                .map {
                    it.getDomAttribute("name") to it.findElements(By.tagName("option"))
                        .filter { it.isSelected }
                        .map { it.getDomAttribute("value") }
                }

            val buttons = associatedFormElements(form, "button")
                .filter { it.getDomAttribute("name") != "" && it == this }
                .map { it.getDomAttribute("name") to listOf(it.getDomAttribute("value")) }

            val ordinaryInputs = inputs + textAreas + selects + buttons
            val addFormModifier = createForm(enctype, ordinaryInputs.toNotNullMap(),
                fileInputs.toNotNullMap())

            val actionString = form.element.attr("action") ?: ""
            val formActionUri = Uri.of(actionString)
            val current = getURL()?.let { Uri.of(it) }
            val formUri = current?.relative(formActionUri) ?: formActionUri
            val postRequest = Request(method, formUri).with(addFormModifier)

            if (method == POST) navigate(postRequest)
            else navigate(Request(method, formUri.query(postRequest.bodyString())).body(""))
        }
    }

    private fun List<Pair<String?, List<String?>>>.toNotNullMap() =
        groupBy({ it.first }, { it.second })
            .mapValues { (_, value: List<List<String?>>) -> value.flatten() }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
            .mapValues { it.value.filterNotNull() }

    private fun WebElement.isAFileInput() = getDomAttribute("type") == "file"
    private fun WebElement.isSubmitInput() = getDomAttribute("type") == "submit"

    private fun WebElement.isAnInactiveSubmitInput() =
        if (isSubmitInput()) {
            this != this@JSoupWebElement
        } else {
            false
        }

    private fun isUncheckedInput(input: WebElement): Boolean =
        (listOf("checkbox", "radio").contains(input.getDomAttribute("type") ?: null)) && input.getDomAttribute("checked") == null

    override fun getLocation(): Point = throw FeatureNotImplementedYet

    override fun <X : Any> getScreenshotAs(target: OutputType<X>): X = throw FeatureNotImplementedYet

    override fun click() {
        when {
            isA("a") -> navigate(Request(GET, element.attr("href")))

            isA("input") && element.attr("type") == "checkbox" ->
                if (isSelected) clear() else element.attr("checked", "checked")

            isA("input") && element.attr("type") == "radio" -> {
                if (element.hasAttr("name")) {
                    current("form")?.findElements(By.tagName("input"))
                        ?.filter { it.getDomAttribute("name") == element.attr("name") }?.forEach { it.clear() }
                }
                element.attr("checked", "checked")
            }

            isA("input") -> {
                if (isDisabled()) return
                val t = element.attr("type")
                if (t == "" || t.lowercase(getDefault()) == "submit")
                    submit()
            }

            isA("option") -> {
                val currentSelectIsMultiple = current("select")?.element?.hasAttr("multiple") == true

                val oldValue = isSelected

                if (currentSelectIsMultiple) element.attr("selected", "selected")
                else current("select")?.findElements(By.tagName("option"))?.forEach { it.clear() }

                if (oldValue && !currentSelectIsMultiple) clear()
                else element.attr("selected", "selected")
            }

            isA("button") -> {
                if (isDisabled()) return
                val t = element.attr("type")
                if (t == "" || t.lowercase(getDefault()) == "submit")
                    submit()
            }
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

    override fun getCssValue(propertyName: String): String = throw FeatureNotImplementedYet

    override fun hashCode(): Int = element.hashCode()

    override fun findElement(by: By) =
        JSoupElementFinder(navigate, getURL, element).findElement(by)

    override fun findElements(by: By) =
        JSoupElementFinder(navigate, getURL, element).findElements(by)

    private fun current(tag: String): JSoupWebElement? = if (isA(tag)) this else parent()?.current(tag)

    private fun associatedForm(): JSoupWebElement? {
        val formId = getDomAttribute("form")
        return if (formId?.isNotBlank() == true) {
            element.root().getElementById(formId)?.let { JSoupWebElement(navigate, getURL, it) }
        } else {
            current("form")
        }
    }

    private fun parent(): JSoupWebElement? = element.parent()?.let { JSoupWebElement(navigate, getURL, it) }

    private fun isA(tag: String) = tagName.lowercase(getDefault()) == tag.lowercase(getDefault())

    private fun associatedFormElements(form: JSoupWebElement, tagName: String): List<WebElement> {
        val root = JSoupWebElement(navigate, getURL, form.element.root())
        val formId: String? = form.getDomAttribute("id")?.ifBlank { null }

        return form.findElements(By.tagName(tagName)) + (formId?.let { root.findElements(By.cssSelector("$tagName[form=$formId]")) }
            ?: emptyList())
    }

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

private fun WebElement.isNotDisabled(): Boolean = this.getDomAttribute("disabled") == null
private fun WebElement.isDisabled() = !isNotDisabled()

private fun createForm(
    enctype: String,
    fileFields: Map<String, List<String>>,
    otherFields: Map<String, List<String>>
): (Request) -> Request {
    return when (enctype) {
        ContentType.MULTIPART_FORM_DATA.value -> createFormMultipart(fileFields, otherFields)
        else -> createFormUrlEncoded(otherFields + fileFields)
    }
}

private fun createFormMultipart(
    otherFields: Map<String, List<String>>,
    fileFields: Map<String, List<String>>
): (Request) -> Request {
    val fields = otherFields.toList()
        .groupBy { it.first }
        .mapValues { (_, values) -> values.flatMap { it.second.map { MultipartFormField(it) } } }

    val files = fileFields.toList()
        .groupBy { it.first }
        .mapValues { (_, values) ->
            values.flatMap {
                it.second.map { filepath ->

                    val file = File(filepath)
                    val contentType: String? = Files.probeContentType(file.toPath())

                    MultipartFormFile(
                        filename = file.name,
                        contentType = contentType?.let { ContentType(it) } ?: ContentType.TEXT_PLAIN,
                        content = file.inputStream()
                    )
                }
            }
        }

    val form = MultipartForm(
        fields = fields,
        files = files
    )

    val body = Body.multipartForm(
        Validator.Strict,
        *(form.fields.map { MultipartFormField.multi.optional(it.key) }.toTypedArray()),
        *(form.fields.map { MultipartFormFile.multi.optional(it.key) }.toTypedArray())
    ).toLens()

    return body.of(form)
}

private fun createFormUrlEncoded(
    fields: Map<String, List<String>>,
): (Request) -> Request {
    val form = WebForm(
        fields.toList()
            .groupBy { it.first }
            .mapValues { it.value.map { it.second }.flatten() }
    )

    val body = Body.webForm(
        Validator.Strict,
        *(form.fields.map { FormField.multi.optional(it.key) }.toTypedArray())
    ).toLens()

    return body.of(form)
}



