package org.http4k.connect.plugin

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.ksp.writeTo
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.Http4kConnectApiClient

class Http4kConnectApiClientKspProcessor(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val allActions = resolver
            .getSymbolsWithAnnotation(Http4kConnectAction::class.qualifiedName!!)
            .toList()

        resolver
            .getSymbolsWithAnnotation(Http4kConnectApiClient::class.qualifiedName!!)
            .forEach {
                it.accept(Http4kConnectApiClientVisitor { logger.info(it.toString()) }, allActions)
                    .forEach { it.writeTo(codeGenerator = codeGenerator, aggregating = false) }
            }
        return emptyList()
    }
}
