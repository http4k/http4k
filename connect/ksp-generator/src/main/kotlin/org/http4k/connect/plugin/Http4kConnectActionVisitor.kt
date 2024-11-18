package org.http4k.connect.plugin

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.ClassKind.OBJECT
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSEmptyVisitor
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import dev.forkhandles.result4k.Result4k
import org.http4k.connect.PagedAction
import org.http4k.connect.RemoteFailure
import java.util.Locale

class Http4kConnectActionVisitor(private val log: (Any?) -> Unit) :
    KSEmptyVisitor<KSClassDeclaration, Sequence<FunSpec>>() {
    override fun visitClassDeclaration(
        classDeclaration: KSClassDeclaration,
        data: KSClassDeclaration
    ): Sequence<FunSpec> {
        log("Processing " + classDeclaration.asStarProjectedType().declaration.qualifiedName!!.asString())

        return classDeclaration.getConstructors()
            .filter { it.isPublic() }
            .flatMap { ctr ->
                listOfNotNull(
                    generateActionExtension(classDeclaration, data, ctr),
                    classDeclaration.takeIf {
                        it.getAllSuperTypes()
                            .map { it.declaration.qualifiedName!!.asString() }
                            .contains(PagedAction::class.qualifiedName)
                    }
                        ?.let { generateActionPagination(classDeclaration, data, ctr) }
                )
            }
    }

    override fun defaultHandler(node: KSNode, data: KSClassDeclaration) = error("unsupported")
}

private fun generateActionPagination(
    actionClass: KSClassDeclaration,
    clientClazz: KSClassDeclaration,
    ctr: KSFunctionDeclaration
) = generateExtensionFunction(
    actionClass, clientClazz, ctr, "Paginated",
    CodeBlock.of(
        "return org.http4k.connect.paginated(::invoke, %T(${ctr.parameters.joinToString(", ") { it.name!!.asString() }}))",
        actionClass.asType(emptyList()).toTypeName()
    ),
    Sequence::class.asClassName().parameterizedBy(
        Result4k::class.asClassName().parameterizedBy(
            (actionClass.getAllFunctions()
                .first { it.simpleName.getShortName() == "toResult" }
                .returnType!!.resolve().arguments[0].type!!.resolve().declaration as KSClassDeclaration)
                .getAllProperties().first { it.simpleName.getShortName() == "items" }.type.toTypeName(),
            RemoteFailure::class.asTypeName()
        )
    )
)

private fun generateActionExtension(
    actionClass: KSClassDeclaration,
    clientClazz: KSClassDeclaration,
    ctr: KSFunctionDeclaration
) = generateExtensionFunction(
    actionClass, clientClazz, ctr, "",
    CodeBlock.of(
        when (actionClass.classKind) {
            OBJECT -> "return invoke(%T)"
            else -> "return invoke(%T(${ctr.parameters.joinToString(", ") { it.name!!.asString() }}))"
        },
        actionClass.asType(emptyList()).toTypeName()
    ),
    actionClass.getAllFunctions()
        .first { it.simpleName.getShortName() == "toResult" }.returnType!!.toTypeName()
)

private fun generateExtensionFunction(
    actionClazz: KSClassDeclaration,
    adapterClazz: KSClassDeclaration,
    ctr: KSFunctionDeclaration,
    suffix: String,
    codeBlock: CodeBlock,
    returnType: TypeName
): FunSpec {
    val baseFunction = FunSpec.builder(
        actionClazz.simpleName.asString().replaceFirstChar { it.lowercase(Locale.getDefault()) } + suffix)
        .addKdoc("@see ${actionClazz.qualifiedName!!.asString().replace('/', '.')}")
        .receiver(adapterClazz.toClassName())
        .returns(returnType)
        .addCode(codeBlock)

    ctr.parameters.forEach {
        val base = ParameterSpec.builder(it.name!!.asString(), it.type.toTypeName())
        with(it.type.resolve()) {
            if (isMarkedNullable) base.defaultValue(CodeBlock.of("null"))
            else if (it.hasDefault) {
                if (starProjection().toString() == "Map<*, *>") base.defaultValue(CodeBlock.of("emptyMap()"))
                else if (starProjection().toString() == "List<*>") base.defaultValue(CodeBlock.of("emptyList()"))
                else if (starProjection().toString() == "Set<*>") base.defaultValue(CodeBlock.of("emptySet()"))
                else {
                }
            } else {
            }
        }
        baseFunction.addParameter(base.build())
    }
    return baseFunction.build()
}
