package org.http4k.connect.plugin

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.visitor.KSEmptyVisitor
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import java.util.Locale

class Http4kConnectApiClientVisitor(private val log: (Any?) -> Unit) :
    KSEmptyVisitor<List<KSAnnotated>, List<FileSpec>>() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: List<KSAnnotated>) =
        classDeclaration.http4kConnectActionTypes.map(KSTypeReference::resolve).map {
            log(
                "Processing http4k-connect adapter: " + classDeclaration.simpleName.asString() +
                    " with action type: $it"
            )
            FileSpec.builder(
                it.declaration.packageName.asString(),
                classDeclaration.simpleName.asString().lowercase(Locale.getDefault()) + "Extensions"
            )
                .apply {
                    data.filterForActionsOf(it)
                        .flatMap { it.accept(Http4kConnectActionVisitor(log), classDeclaration) }
                        .forEach(::addFunction)
                }
                .build()

        }.toList()

    override fun defaultHandler(node: KSNode, data: List<KSAnnotated>) = error("unsupported")
}

internal val KSClassDeclaration.http4kConnectActionTypes
    get() = getAllFunctions()
        .filter { it.simpleName.getShortName() == "invoke" }
        .map { it.parameters.first().type }


fun List<KSAnnotated>.filterForActionsOf(actionType: KSType) =
    filterIsInstance<KSClassDeclaration>()
        .filter {
            it.getAllSuperTypes().map(KSType::starProjection)
                .contains(actionType.starProjection())
        }
