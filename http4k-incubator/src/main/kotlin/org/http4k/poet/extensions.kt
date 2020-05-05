package org.http4k.poet

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import org.http4k.lens.Cookies
import org.http4k.lens.Header
import org.http4k.lens.LensSpec
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.openapi.OpenApi3Spec
import org.http4k.openapi.ParameterSpec
import org.http4k.openapi.PathSpec
import kotlin.reflect.KClass

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

fun OpenApi3Spec.lensDeclarations(pathSpec: PathSpec) = pathSpec.parameters.map {
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
