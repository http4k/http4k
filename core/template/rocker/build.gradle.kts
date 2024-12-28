import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Rocker templating support"

val license by project.extra { Apache2 }


plugins {
    id("nu.studer.rocker")
    id("org.http4k.community")
}

apply(plugin = "java")
apply(plugin = "nu.studer.rocker")

dependencies {
    api(project(":http4k-template-core"))
    api("com.fizzed:rocker-runtime:_")

    testImplementation("com.fizzed:rocker-compiler:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-template-core")))
}

rocker {
    version.set("1.3.0")
    configurations {
        create("test") {
            templateDir.set(file("src/test/resources"))
            outputDir.set(file("src/test/generated/kotlin"))
            classDir.set(file("out/test/classes"))
            extendsModelClass.set("org.http4k.template.RockerViewModel")
        }
    }
}
