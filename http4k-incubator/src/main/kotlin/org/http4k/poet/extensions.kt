package org.http4k.poet

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.format.Jackson
import org.http4k.lens.Cookies
import org.http4k.lens.FormField
import org.http4k.lens.Header
import org.http4k.lens.LensSpec
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.Validator
import org.http4k.lens.WebForm
import org.http4k.openapi.NamedSchema
import org.http4k.openapi.SchemaSpec
import org.http4k.openapi.cleanValueName
import org.http4k.openapi.v3.OpenApi3ParameterSpec
import org.http4k.openapi.v3.OpenApi3ParameterSpec.CookieSpec
import org.http4k.openapi.v3.OpenApi3ParameterSpec.FormFieldSpec
import org.http4k.openapi.v3.OpenApi3ParameterSpec.HeaderSpec
import org.http4k.openapi.v3.OpenApi3ParameterSpec.PathSpec
import org.http4k.openapi.v3.OpenApi3ParameterSpec.QuerySpec
import org.http4k.openapi.v3.OpenApi3ParameterSpec.RefSpec
import kotlin.reflect.KClass


fun OpenApi3ParameterSpec.asTypeName() = when (schema) {
    is SchemaSpec.ArraySpec -> List::class.asClassName().parameterizedBy(schema.itemsSpec().clazz!!.asClassName())
    else -> schema.clazz!!.asClassName().copy(nullable = !required)
}

fun FileSpec.Builder.buildFormatted() = this.indent("\t").build()

fun OpenApi3ParameterSpec.quotedName() = "\"$name\""

val OpenApi3ParameterSpec.lensSpecClazz
    get() = when (this) {
        is CookieSpec -> Cookies::class
        is HeaderSpec -> Header::class
        is QuerySpec -> Query::class
        is PathSpec -> Path::class
        is FormFieldSpec -> FormField::class
        is RefSpec -> throw IllegalStateException()
    }

inline fun <reified T : Any> member(name: String) = MemberName(T::class.asClassName(), name)
inline fun <reified T : Any> packageMember(name: String) = T::class.packageMember(name)
fun KClass<*>.packageMember(name: String) = MemberName(qualifiedName!!.split(".").dropLast(1).joinToString("."), name)

fun FunSpec.Builder.addCodeBlocks(blocks: List<CodeBlock>) = blocks.fold(this) { acc, next -> acc.addCode("\n").addCode(next) }.addCode("\n")

fun OpenApi3ParameterSpec.lensConstruct() =
    when (this) {
        is PathSpec -> "of"
        else -> if (required) "required" else "optional"
    }

fun org.http4k.openapi.v3.Path.responseLensDeclarations(modelPackageName: String) =
    responseSchemas().mapNotNull { it.lensDeclaration(modelPackageName) }

fun org.http4k.openapi.v3.Path.requestLensDeclarations(modelPackageName: String) =
    requestSchemas().mapNotNull { it.lensDeclaration(modelPackageName) }

fun org.http4k.openapi.v3.Path.buildWebForm() = when {
    supportsFormContent() -> {
        val with = packageMember<Filter>("with")
        val buildForm = CodeBlock.builder().addStatement("val request = %T()", WebForm::class.asClassName()).indent()
        formFields().forEach { buildForm.addStatement(".%M(${it.name}Lens of ${it.name})", with) }

        listOfNotNull(buildForm.unindent().build())
    }
    else -> emptyList()
}

fun org.http4k.openapi.v3.Path.supportsFormContent() = formFields().isNotEmpty()

private fun org.http4k.openapi.v3.Path.formFields() = spec.parameters.filterIsInstance<FormFieldSpec>()

fun org.http4k.openapi.v3.Path.webFormLensDeclaration(): List<CodeBlock> =
    when {
        supportsFormContent() ->
            listOf(
                CodeBlock.of(
                    "val formLens = %T.%M(%M, ${formFields().joinToString(", ") { it.name + "Lens" }}).toLens()",
                    Body::class.asTypeName(),
                    packageMember<LensSpec<*, *>>("webForm"),
                    member<Validator>("Strict")
                )
            )
        else -> emptyList()
    }

fun org.http4k.openapi.v3.Path.parameterLensDeclarations() = spec.parameters
    .map {
        when (it) {
            is CookieSpec -> CodeBlock.of(
                "val ${it.name}Lens = %T.${it.lensConstruct()}(${it.quotedName()})",
                it.lensSpecClazz.asClassName()
            )
            else -> when (it.schema) {
                is SchemaSpec.ArraySpec -> CodeBlock.of(
                    "val ${it.name}Lens = %T.%M().multi.${it.lensConstruct()}(${it.quotedName()})",
                    it.lensSpecClazz.asClassName(),
                    packageMember<LensSpec<*, *>>(it.schema.itemsSpec().clazz!!.simpleName!!.decapitalize())
                )
                else -> CodeBlock.of(
                    "val ${it.name}Lens = %T.%M().${it.lensConstruct()}(${it.quotedName()})",
                    it.lensSpecClazz.asClassName(),
                    packageMember<LensSpec<*, *>>(it.schema.clazz!!.simpleName!!.decapitalize())
                )
            }
        }
    }

fun NamedSchema.lensDeclaration(modelPackageName: String): CodeBlock? = when (this) {
    is NamedSchema.Generated -> {
        val modelClassName = modelPackageName.childClassName(name)
        when (schema) {
            is SchemaSpec.ObjectSpec -> autoLensBlock(modelClassName)
            is SchemaSpec.ArraySpec -> autoLensBlock(List::class.asClassName().parameterizedBy(modelClassName))
            is SchemaSpec.RefSpec -> autoLensBlock(modelClassName)
            else -> null
        }
    }
    is NamedSchema.Existing -> autoLensBlock(typeName)
}

private fun NamedSchema.autoLensBlock(type: TypeName) = CodeBlock.of(
    "val ${fieldName}Lens = %T.%M<%T>().toLens()",
    Body::class.asTypeName(),
    member<Jackson>("auto"),
    type
)

fun ClassName.sibling(siblingName: String): ClassName = ClassName(packageName, siblingName.cleanValueName().capitalize())

fun String.childClassName(childName: String) = ClassName(this, childName.cleanValueName().capitalize())
