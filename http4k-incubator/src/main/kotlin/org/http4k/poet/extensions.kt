package org.http4k.poet

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import org.http4k.core.Body
import org.http4k.format.Jackson
import org.http4k.lens.Cookies
import org.http4k.lens.Header
import org.http4k.lens.LensSpec
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.openapi.v3.OpenApi3Spec
import org.http4k.openapi.v3.ParameterSpec
import org.http4k.openapi.v3.SchemaSpec
import kotlin.reflect.KClass
import org.http4k.openapi.v3.Path as CPath

fun ParameterSpec.asTypeName() = schema.clazz?.asTypeName()?.copy(nullable = !required)

fun FileSpec.Builder.buildFormatted() = this.indent("\t").build()

fun ParameterSpec.quotedName() = "\"$name\""

val ParameterSpec.lensSpecClazz
    get() = when (this) {
        is ParameterSpec.CookieSpec -> Cookies::class
        is ParameterSpec.HeaderSpec -> Header::class
        is ParameterSpec.QuerySpec -> Query::class
        is ParameterSpec.PathSpec -> Path::class
    }

inline fun <reified T : Any> member(name: String) = MemberName(T::class.asClassName(), name)
inline fun <reified T : Any> packageMember(name: String) = T::class.packageMember(name)
fun KClass<*>.packageMember(name: String) = MemberName(qualifiedName!!.split(".").dropLast(1).joinToString("."), name)

fun FunSpec.Builder.addCodeBlocks(blocks: List<CodeBlock>) = blocks.fold(this) { acc, next -> acc.addCode("\n").addCode(next) }.addCode("\n")

fun ParameterSpec.lensConstruct() =
    when (this) {
        is ParameterSpec.PathSpec -> "of"
        else -> if (required) "required" else "optional"
    }

fun OpenApi3Spec.lensDeclarations(path: CPath): List<CodeBlock> {
    val bodies = path.pathSpec.requestBody?.content?.entries
        ?.mapNotNull { it.value.schema }
        ?.mapNotNull {
            when (it) {
                is SchemaSpec.ObjectSpec -> {
                    CodeBlock.of(
                        "val bodyLens = %T.%M<Any>().toLens()",
                        Body::class.asTypeName(),
                        member<Jackson>("auto")
                    )
                }
                is SchemaSpec.ArraySpec -> {
                    CodeBlock.of(
                        "val bodyLens = %T.%M<List<Any>>().toLens()",
                        Body::class.asTypeName(),
                        member<Jackson>("auto")
                    )
                }
                is SchemaSpec.RefSpec -> {
                    CodeBlock.of(
                        "val bodyLens = %T.%M<Any>().toLens()",
                        Body::class.asTypeName(),
                        member<Jackson>("auto")
                    )
                }
                else -> null
            }
        } ?: emptyList()

    val parameters = path.pathSpec.parameters.map {
        when (it) {
            is ParameterSpec.CookieSpec -> CodeBlock.of(
                "val ${it.name}Lens = %T.${it.lensConstruct()}(${it.quotedName()})",
                it.lensSpecClazz.asClassName()
            )
            else -> CodeBlock.of(
                "val ${it.name}Lens = %T.%M().${it.lensConstruct()}(${it.quotedName()})",
                it.lensSpecClazz.asClassName(),
                packageMember<LensSpec<*, *>>(it.schema.clazz!!.simpleName!!.toLowerCase())
            )
        }
    }

    return bodies + parameters
}
