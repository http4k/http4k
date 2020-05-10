object Config {
    val TestDependencies: List<String> = listOf(
        Libs.junit_jupiter_api, Libs.junit_jupiter_engine, Libs.kotlin_reflect, Libs.hamkrest
    )

    val jackson_major_minor = Versions.jackson_dataformat_xml.major_minor()

    val moshi_major = Versions.com_squareup_moshi.major()

    val okhttp_major = Versions.okhttp.major()

    val netty_codec_http2_major_minor = Versions.netty_codec_http2.major_minor()

    val io_undertow_major_minor = Versions.io_undertow.major_minor()

    private fun String.major(): String =
        this.substringBefore(".")

    private fun String.major_minor(): String {
        val (major, minor) = this.split(".")
        return "$major.$minor"
    }
}
