package org.http4k.format

import com.squareup.moshi.Moshi
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_YAML
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.ContentNegotiation.Companion.None
import org.http4k.lens.string
import org.http4k.websocket.WsMessage
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK
import org.yaml.snakeyaml.Yaml
import java.io.InputStream
import kotlin.reflect.KClass

open class ConfigurableMoshiYaml(
    builder: Moshi.Builder, val defaultContentType: ContentType = APPLICATION_YAML,
    private val yamlDumperOptions: DumperOptions = defaultDumperOptions
) :
    AutoMarshalling() {
    private val json = ConfigurableMoshi(builder, defaultContentType)

    override fun <T : Any> asA(input: InputStream, target: KClass<T>): T =
        json.asA(json.asFormatString(yaml().load<Map<String, Any>>(input)), target)

    override fun <T : Any> asA(input: String, target: KClass<T>) = asA(input.byteInputStream(), target)

    override fun asFormatString(input: Any): String {
        val str = json.asFormatString(input)
        val yaml = yaml()

        return try {
            yaml.dump(json.asA<Map<String, Any>>(str))
        } catch (e: Exception) {
            e.printStackTrace()
            yaml.dump(str)
        }
    }

    private fun yaml() = Yaml(yamlDumperOptions)

    inline fun <reified T : Any> WsMessage.Companion.auto() = WsMessage.string().map({ }, ::asFormatString)

    inline fun <reified T : Any> Body.Companion.auto(
        description: String? = null,
        contentNegotiation: ContentNegotiation = None
    ) = autoBody<T>(description, contentNegotiation)

    inline fun <reified T : Any> autoBody(
        description: String? = null,
        contentNegotiation: ContentNegotiation = None
    ): BiDiBodyLensSpec<T> = httpBodyLens(
        description, contentNegotiation,
        defaultContentType
    ).map({ asA(it) }, ::asFormatString)
}

val defaultDumperOptions = DumperOptions().apply {
    indent = 2
    isPrettyFlow = true
    defaultScalarStyle = DumperOptions.ScalarStyle.PLAIN
    defaultFlowStyle = BLOCK
}

