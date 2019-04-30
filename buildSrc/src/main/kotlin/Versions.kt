/**
 * Find which updates are available by running
 *     `$ ./gradlew syncLibs`
 * This will only update the comments.
 *
 * YOU are responsible for updating manually the dependency version. */
object Versions {
    const val jackson_dataformat_xml: String = "2.9.8"

    const val jackson_module_kotlin: String = "2.9.8"

    const val jackson_module_jsonSchema: String = "2.9.8"

    const val underscore: String = "1.43"

    const val handlebars: String = "4.1.2"

    const val com_github_kt3k_coveralls_gradle_plugin: String = "2.8.2"

    const val gson: String = "2.8.5"

    const val com_jfrog_bintray_gradle_plugin: String = "1.8.4"

    const val hamkrest: String = "1.7.0.0"

    const val result4k: String = "2.0.0"

    const val com_squareup_moshi: String = "1.8.0"

    const val okhttp: String = "3.14.1"

    const val io_github_resilience4j: String = "0.14.1"

    const val ktor_server_cio: String = "1.1.4"

    const val micrometer_core: String = "1.1.4"

    const val netty_codec_http2: String = "4.1.35.Final"

    const val pebble: String = "3.0.8"

    const val io_undertow: String = "2.0.20.Final"

    const val javax_servlet_api: String = "4.0.1"

    const val jmfayard_github_io_gradle_kotlin_dsl_libs_gradle_plugin: String = "0.2.6"

    const val nebula_provided_base_gradle_plugin: String = "3.0.3" //available: "5.0.3"

    const val net_saliman_cobertura_gradle_plugin: String = "2.6.1"

    const val gradle_cobertura_plugin: String = "2.6.1"

    const val argo: String = "5.5"

    const val cobertura: String = "2.1.1"

    const val commons_pool2: String = "2.6.2"

    const val httpasyncclient: String = "4.1.4"

    const val httpclient: String = "4.5.8"

    const val httpcore: String = "4.4.11"

    const val http2_server: String = "9.4.17.v20190418"

    const val javax_websocket_server_impl: String = "9.4.17.v20190418"

    const val org_eclipse_jetty: String = "9.4.17.v20190418"

    const val freemarker: String = "2.3.28"

    const val java_websocket: String = "1.4.0"

    const val dokka_gradle_plugin: String = "0.9.18"

    const val org_jetbrains_kotlin: String = "1.3.31"

    const val json: String = "20180813"

    const val jsoup: String = "1.11.3"

    const val org_junit_jupiter: String = "5.4.2"

    const val coveralls_gradle_plugin: String = "2.8.2"

    const val alpn_boot: String = "8.1.13.v20181017"

    const val selenium_api: String = "3.141.59"

    const val thymeleaf: String = "3.0.11.RELEASE"

    /**
     *
     *   To update Gradle, edit the wrapper file at path:
     *      ./gradle/wrapper/gradle-wrapper.properties
     */
    object Gradle {
        const val runningVersion: String = "5.0"

        const val currentVersion: String = "5.4.1"

        const val nightlyVersion: String = "5.5-20190428000050+0000"

        const val releaseCandidate: String = ""
    }
}
