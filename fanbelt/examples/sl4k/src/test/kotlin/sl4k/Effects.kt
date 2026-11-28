package sl4k

import org.http4k.webdriver.datastar.AttributePlugin
import org.http4k.webdriver.datastar.DatastarAttribute
import org.http4k.webdriver.datastar.DatastarDomEvent
import org.http4k.webdriver.datastar.DatastarPage
import org.jsoup.nodes.Element

/**
 * The data-effect attribute, for the emulated browser.
 */
object Effects : AttributePlugin {
    override val name = "effect"

    private val fileNames = Regex("""^\s*el\.textContent\s*=\s*\$\w+\.map\(file => file\.name\)\.join\(', '\)\s*;?\s*$""")

    override fun render(element: Element, attribute: DatastarAttribute, page: DatastarPage) {
        if (!fileNames.matches(attribute.expression)) page.evaluate(attribute, DatastarDomEvent("effect", element))
    }
}
