package org.http4k.template.dust

import java.io.File


fun loadFromFilesIn(baseDirectory: String): TemplateLoader =
    loadFromFilesIn(File(baseDirectory))


fun loadFromFilesIn(baseDirectory: File): TemplateLoader =
    fun(name: String) =
        File(baseDirectory, "$name.dust").takeIf(File::exists)?.readText()


fun loadFromResourcesIn(pkg: Package) =
    loadFromResourcesIn(pkg.name)


fun loadFromResourcesIn(baseClasspathPackage: String): TemplateLoader {
    val resourceRoot = baseClasspathPackage.replace('.', '/')
    return fun(templateName: String) =
        ClassLoader.getSystemClassLoader().getResource(resourcePath(resourceRoot, templateName))?.readText()
}

private fun resourcePath(resourceRoot: String, templateName: String) =
    (if (resourceRoot.isEmpty()) "" else resourceRoot + "/") + templateName + ".dust"


inline fun <reified T : Any> loadFromResourcesOf(): TemplateLoader =
    loadFromResourcesOf(T::class.java)


fun loadFromResourcesOf(cls: Class<*>): TemplateLoader =
    fun(templateName: String) =
        cls.getResource("$templateName.dust")?.readText()
