package org.http4k.openapi.v3.models

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.openapi.v3.ApiGenerator
import org.http4k.openapi.v3.GenerationOptions
import org.http4k.openapi.v3.OpenApi3Spec
import org.http4k.openapi.v3.PathSpec
import org.http4k.poet.buildFormatted

object ModelApiGenerator : ApiGenerator {
    override fun invoke(spec: OpenApi3Spec, options: GenerationOptions): List<FileSpec> = with(spec) {

        val componentSchemas = components.schemas.entries.fold(mutableMapOf<String, TypeSpec>()) { acc, (name, schema) ->
            acc += (name.capitalize() to acc.getOrDefault(name.capitalize(), schema.buildModelClass(name, components.schemas, acc)))
            acc
        }.values

        val formSchemas = spec.paths.flatMap { (path: String, verbToPathSpec: Map<String, PathSpec>) ->
            verbToPathSpec.mapNotNull { (method, pathSpec) ->
                val functionName = pathSpec.operationId ?: method.toLowerCase() + path.replace('/', '_')
                pathSpec.requestBody
                    ?.contentFor(APPLICATION_FORM_URLENCODED)
                    ?.schema
                    ?.buildModelClass(functionName + "Form", emptyMap(), mutableMapOf())
            }
        }

        (componentSchemas + formSchemas).map {
            FileSpec.builder(options.packageName("model"), it.name!!)
                .addType(it)
                .buildFormatted()
        }
    }
}
