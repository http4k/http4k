package settings

import java.io.File

/**
 * This creates a directory contain XML files for custom IntelliJ settings
 */
fun main() {
    File("intellij-settings").apply {
        mkdirs()
        writeLiveTemplates()
    }.also { println("Wrote settings to: " + it.absolutePath) }
}

