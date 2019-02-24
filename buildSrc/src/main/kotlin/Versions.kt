/**
 * Find which updates are available by running
 *     `$ ./gradlew syncLibs`
 * This will only update the comments.
 *
 * YOU are responsible for updating manually the dependency version. */
object Versions {
    const val jackson_module_kotlin = "2.9.8"

    const val handlebars = "4.1.2"

    const val com_github_kt3k_coveralls_gradle_plugin = "2.8.2"

    const val gson = "2.8.5"

    const val com_jfrog_bintray_gradle_plugin = "1.8.4"

    const val hamkrest = "1.7.0.0"

    const val com_squareup_moshi = "1.8.0"

    const val okhttp = "3.12.1" //available: "3.13.1"

    const val rerunner_jupiter = "1.1.1"

    const val io_github_resilience4j = "0.13.2"

    const val ktor_server_cio = "1.1.3"

    const val micrometer_core = "1.1.3"

    const val netty_codec_http2 = "4.1.33.Final"

    const val pebble = "3.0.8"

    const val io_undertow = "2.0.19.Final"

    const val javax_servlet_api = "4.0.1"

    const val jmfayard_github_io_gradle_kotlin_dsl_libs_gradle_plugin = "0.2.6"

    const val nebula_provided_base_gradle_plugin = "3.0.3" //available: "5.0.0"

    const val net_saliman_cobertura_gradle_plugin = "2.6.0"

    const val gradle_cobertura_plugin = "2.6.0"

    const val argo = "5.5"

    const val cobertura = "2.1.1"

    const val commons_pool2 = "2.6.1"

    const val httpasyncclient = "4.1.4"

    const val httpclient = "4.5.7"

    const val httpcore = "4.4.11"

    const val http2_server = "9.4.15.v20190215"

    const val javax_websocket_server_impl = "9.4.15.v20190215"

    const val org_eclipse_jetty = "9.4.15.v20190215"

    const val java_websocket = "1.4.0"

    const val dokka_gradle_plugin = "0.9.17"

    const val org_jetbrains_kotlin = "1.3.21"

    const val json = "20180813"

    const val jsoup = "1.11.3"

    const val org_junit_jupiter = "5.4.0"

    const val coveralls_gradle_plugin = "2.8.2"

    const val alpn_boot = "8.1.13.v20181017"

    const val selenium_api = "3.141.59"

    const val thymeleaf = "3.0.11.RELEASE"

    /**
     *
     *   To update Gradle, edit the wrapper file at path:
     *      ./gradle/wrapper/gradle-wrapper.properties
     */
    object Gradle {
        const val runningVersion = "5.0"

        const val currentVersion = "5.2.1"

        const val nightlyVersion = "5.4-20190224000056+0000"

        const val releaseCandidate = ""
    }
}
