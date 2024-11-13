plugins {
    id("org.http4k.nexus-config")
    id("org.http4k.conventions")
}

allprojects {
    apply(plugin = "org.http4k.conventions")
    apply(plugin = "org.http4k.code-coverage")

    repositories {
        mavenCentral()
    }
}
