package org.http4k.template.dust

import java.io.File


internal fun loadFromFilesIn(baseDirectory: String): TemplateLoader =
    loadFromFilesIn(File(baseDirectory))


internal fun loadFromFilesIn(baseDirectory: File): TemplateLoader =
    fun(name: String) =
        File(baseDirectory, "$name.dust").takeIf(File::exists)?.readText()


internal fun loadFromResourcesIn(baseClasspathPackage: String): TemplateLoader {
    val resourceRoot = baseClasspathPackage.replace('.', '/')
    return fun(templateName: String) =
        ClassLoader.getSystemClassLoader().getResource(resourcePath(resourceRoot, templateName))?.readText()
}

private fun resourcePath(resourceRoot: String, templateName: String) =
    (if (resourceRoot.isEmpty()) "" else "$resourceRoot/") + templateName + ".dust"
