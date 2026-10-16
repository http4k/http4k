package org.http4k.connect.plugin

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class KotshiProguardRulesKspProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) =
        KotshiProguardRulesKspProcessor(environment.codeGenerator)
}

class KotshiProguardRulesKspProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {

    private val processed = mutableSetOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation("se.ansman.kotshi.JsonSerializable")
            .filterIsInstance<KSClassDeclaration>()
            .filter { processed.add(it.binaryName()) }
            .forEach { it.writeProguardRules() }
        return emptyList()
    }

    private fun KSClassDeclaration.writeProguardRules() {
        codeGenerator.createNewFile(
            Dependencies(false, *listOfNotNull(containingFile).toTypedArray()),
            "META-INF.proguard",
            "http4k-kotshi-${binaryName()}",
            "pro"
        ).bufferedWriter().use {
            it.write(
                """
                -keepnames class kotlin.jvm.internal.DefaultConstructorMarker
                -keepclassmembers class ${binaryName()} {
                    synthetic <init>(...);
                }

                """.trimIndent()
            )
        }
    }
}

private fun KSClassDeclaration.binaryName(): String {
    val pkg = packageName.asString()
    val nested = qualifiedName!!.asString().removePrefix(pkg).removePrefix(".").replace('.', '$')
    return if (pkg.isEmpty()) nested else "$pkg.$nested"
}
