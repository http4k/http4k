package org.http4k.connect.plugin

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ksp.writeTo
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.Http4kConnectApiClient

class Http4kConnectApiClientKspProcessor(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator
) : SymbolProcessor {

    // Track completed clients across processor rounds
    private val processedClients = mutableSetOf<String>()

    private val visitor = Http4kConnectApiClientVisitor { logger.info("http4k-connect: $it") }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // bypass dirty file filter, which is too strict on some systems
        val declarations = resolver.getAllFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        val allActions = declarations.filter { decl ->
            decl.annotations.any {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == Http4kConnectAction::class.qualifiedName
            }
        }

        val allClients = declarations.filter { decl ->
            decl.annotations.any {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == Http4kConnectApiClient::class.qualifiedName
            }
        }

        // filter out clients processed in previous rounds; not doing so will result in FileAlreadyExistsException
        val remainingClients = allClients.filter { it.qualifiedName?.asString() !in processedClients }
        if (remainingClients.isEmpty()) return emptyList()

        // If any clients have been deferred, retry later
        (remainingClients + allActions)
            .filter { decl -> !decl.validate() || decl.getAllSuperTypes().any { it.isError } }
            .let { if (it.isNotEmpty()) return it }

        val originatingFiles = (allActions + allClients).mapNotNull { it.containingFile }.toTypedArray()

        for (client in remainingClients) {
            client.accept(visitor, allActions)
                .filter { it.members.isNotEmpty() }
                .forEach { fileSpec ->
                    fileSpec.writeTo(
                        codeGenerator = codeGenerator,
                        dependencies = Dependencies(aggregating = true, sources = originatingFiles)
                    )
                }

            client.qualifiedName?.asString()?.let(processedClients::add)
        }

        return emptyList()
    }
}
