description = "Http4k Thymeleaf templating support"

plugins {
    id("nu.studer.rocker")
}

apply(plugin = "java")
apply(plugin = "nu.studer.rocker")

dependencies {
    api(project(":http4k-template-core"))
    api("com.fizzed:rocker-runtime:_")

    testImplementation("com.fizzed:rocker-compiler:_")
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
    testImplementation(project(path = ":http4k-template-core", configuration = "testArtifacts"))
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
