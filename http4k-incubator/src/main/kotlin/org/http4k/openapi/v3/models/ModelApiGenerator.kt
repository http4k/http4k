package org.http4k.openapi.v3.models

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.openapi.v3.ApiGenerator
import org.http4k.openapi.v3.GenerationOptions
import org.http4k.openapi.v3.OpenApi3Spec
import org.http4k.poet.buildFormatted

object ModelApiGenerator : ApiGenerator {
    override fun invoke(spec: OpenApi3Spec, options: GenerationOptions): List<FileSpec> = with(spec) {

        val allSchemas = components.schemas.entries.fold(mutableMapOf<String, TypeSpec>()) { acc, (name, schema) ->
            acc += (name.capitalize() to acc.getOrDefault(name.capitalize(), schema.buildModelClass(name, components.schemas, acc)))
            acc
        }

        spec.paths.entries.fold(allSchemas) { acc, (path, verbToPathSpec) ->
            verbToPathSpec.forEach { (method, pathSpec) ->
                pathSpec.requestBody
                    ?.contentFor(APPLICATION_FORM_URLENCODED)
                    ?.schema
                    ?.also {
                        val functionName = pathSpec.operationId ?: method.toLowerCase() + path.replace('/', '_')
                        val name = functionName + "Form".capitalize()
                        allSchemas += (name to allSchemas.getOrDefault(name, it.buildModelClass(name, components.schemas, allSchemas)))
                    }
            }
            acc
        }

        allSchemas.values.distinct().map {
            FileSpec.builder(options.packageName("model"), it.name!!)
                .addType(it)
                .buildFormatted()
        }
    }
}
