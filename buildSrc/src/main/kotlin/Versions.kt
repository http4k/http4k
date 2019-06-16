/**
 * Find which updates are available by running
 *     `$ ./gradlew syncLibs`
 * This will only update the comments.
 *
 * YOU are responsible for updating manually the dependency version. */
object Versions {
    const val jackson_major_minor: String = "2.9"

    const val jackson_dataformat_xml: String = "$jackson_major_minor.9"

    const val jackson_module_kotlin: String = "$jackson_major_minor.9"

    const val underscore: String = "1.45"

    const val handlebars: String = "4.1.2"

    const val com_github_kt3k_coveralls_gradle_plugin: String = "2.8.4"

    const val gson: String = "2.8.5"

    const val com_jfrog_bintray_gradle_plugin: String = "1.8.4"

    const val hamkrest: String = "1.7.0.0"

    const val result4k: String = "2.0.0"

    const val moshi_major: String = "1"
    const val com_squareup_moshi: String = "$moshi_major.8.0"

    const val okhttp_major: String = "3"
    const val okhttp: String = "$okhttp_major.14.2"

    const val io_github_resilience4j: String = "0.16.0"

    const val ktor_server_cio: String = "1.2.1"

    const val micrometer_core: String = "1.1.4"

    const val netty_codec_http2_major_minor = "4.1"
    const val netty_codec_http2: String = "$netty_codec_http2_major_minor.36.Final"

    const val pebble: String = "3.0.10"

    const val io_undertow_major_minor: String = "2.0"
    const val io_undertow: String = "$io_undertow_major_minor.21.Final"

    const val javax_servlet_api: String = "4.0.1"

    const val jmfayard_github_io_gradle_kotlin_dsl_libs_gradle_plugin: String = "0.2.6"

    const val nebula_provided_base_gradle_plugin: String = "3.0.3" //available: "5.0.3"

    const val net_saliman_cobertura_gradle_plugin: String = "2.6.1"

    const val gradle_cobertura_plugin: String = "2.6.1"

    const val argo: String = "5.5"

    const val cobertura: String = "2.1.1"

    const val commons_pool2: String = "2.6.2"

    const val httpasyncclient: String = "4.1.4"

    const val httpclient: String = "4.5.9"

    const val httpcore: String = "4.4.11"

    const val http2_server: String = "9.4.19.v20190610"

    const val javax_websocket_server_impl: String = "9.4.19.v20190610"

    const val org_eclipse_jetty: String = "9.4.19.v20190610"

    const val freemarker: String = "2.3.28"

    const val java_websocket: String = "1.4.0"

    const val dokka_gradle_plugin: String = "0.9.18"

    const val org_jetbrains_kotlin: String = "1.3.31"

    const val json: String = "20180813"

    const val jsoup: String = "1.12.1"

    const val org_junit_jupiter: String = "5.4.2"

    const val coveralls_gradle_plugin: String = "2.8.3"

    const val alpn_boot: String = "8.1.13.v20181017"

    const val openapi_generator_gradle_plugin: String = "4.0.1"

    const val selenium_api: String = "3.141.59"

    const val thymeleaf: String = "3.0.11.RELEASE"

    /**
     *
     *   To update Gradle, edit the wrapper file at path:
     *      ./gradle/wrapper/gradle-wrapper.properties
     */
    object Gradle {
        const val runningVersion: String = "5.4.1"

        const val currentVersion: String = "5.4.1"

        const val nightlyVersion: String = "5.6-20190616000028+0000"

        const val releaseCandidate: String = "5.5-rc-3"
    }
}
