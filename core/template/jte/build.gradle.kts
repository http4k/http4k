import gg.jte.ContentType
import gg.jte.gradle.GenerateJteTask

description = "http4k JTE templating support"

plugins {
    id("org.http4k.community")
    alias(libs.plugins.jte)
}

dependencies {
    api(project(":http4k-template-core"))
    api(libs.jte)
    api(libs.jte.kotlin)

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-template-core")))
}

val generateTestJte = tasks.register("generateTestJte", GenerateJteTask::class) {
    sourceDirectory = file("src/test/jte").toPath()
    targetDirectory = layout.buildDirectory.dir("generated/sources/jte-test").get().asFile.toPath()
    contentType = ContentType.Html
    packageName = "gg.jte.generated.precompiled"
}

sourceSets {
    test {
        java {
            srcDir(generateTestJte.map { it.targetDirectory })
        }
        kotlin {
            srcDir(generateTestJte.map { it.targetDirectory })
        }
    }
}

tasks.named("compileTestKotlin") {
    dependsOn(generateTestJte)
}
