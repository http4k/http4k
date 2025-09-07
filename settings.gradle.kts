rootProject.name = "http4k"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
    includeBuild("gradle/gradle-plugins")
}


gradle.startParameter.isContinueOnFailure = true

val exclusions = setOf("gradle-plugins")

rootDir.walkTopDown()
    .filter { it.isDirectory && File(it, "build.gradle.kts").exists() }
    .filterNot { dir -> dir == rootDir || exclusions.any { dir.absolutePath.contains(it) } }
    .forEach {
        val module = it.relativeTo(rootDir).path
            .replace(File.separatorChar, '/')
            .removePrefix("core/") // remove core ecosystem modules as they have no prefix to add
            .removePrefix("pro/") // remove pro ecosystem modules as they have no prefix to add
            .replace('/', '-')
        val moduleName = module.takeIf { it.contains("connect") }?.removeSuffix("-client")
            ?: module // replace http4k-connect client module names as they have no suffixx

        include(":http4k-$moduleName")
        project(":http4k-$moduleName").projectDir = File(it.absoluteFile.path)
    }
