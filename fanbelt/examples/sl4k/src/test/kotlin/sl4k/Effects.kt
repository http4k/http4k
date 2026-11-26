package sl4k

import org.http4k.webdriver.datastar.AttributePlugin
import org.http4k.webdriver.datastar.DatastarAttribute

/**
 * Lets the emulated browser accept data-effect expressions, which it otherwise fails on.
 */
object Effects : AttributePlugin {
    override val name = "effect"

    private val focus = Regex("""^\s*\$\w+\s*&&\s*el\.focus\(\)\s*;?\s*$""")
    private val fileNames = Regex("""^\s*el\.textContent\s*=\s*\$\w+\.map\(file => file\.name\)\.join\(', '\)\s*;?\s*$""")

    override fun models(attribute: DatastarAttribute) = focus.matches(attribute.expression) || fileNames.matches(attribute.expression)
}
