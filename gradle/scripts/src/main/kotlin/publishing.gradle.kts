plugins {
    kotlin("jvm")
    `java-library`
    signing
    `maven-publish`
}

val enableSigning = project.findProperty("sign") == "true"

if (enableSigning) { // when added it expects signing keys to be configured
    apply(plugin = "signing")
    signing {
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}
