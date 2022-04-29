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
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Representer
import org.yaml.snakeyaml.resolver.Resolver
import java.io.InputStream
import java.util.regex.Pattern
import kotlin.reflect.KClass

open class ConfigurableMoshiYaml(
    builder: Moshi.Builder, val defaultContentType: ContentType = APPLICATION_YAML,
    private val yamlDumperOptions: DumperOptions = defaultDumperOptions,
    private val resolver: Resolver = MinimalResolver
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
            yaml.dump(str)
        }
    }

    private fun yaml() = Yaml(
        Constructor(), Representer(), yamlDumperOptions, LoaderOptions(), resolver)
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

/**
 * This resolver overrides the default behaviour defined in SnakeYAML (which
 * interprets strings like "on" and "off" as boolean values).
 */
object MinimalResolver : Resolver() {
    override fun addImplicitResolver(tag: Tag, regexp: Pattern, first: String?) =
        when (tag) {
            Tag.BOOL -> super.addImplicitResolver(
                tag,
                Pattern.compile("^(?:true|True|TRUE|false|False|FALSE)$"),
                "tTfF"
            )
            else -> super.addImplicitResolver(tag, regexp, first)
        }
}

