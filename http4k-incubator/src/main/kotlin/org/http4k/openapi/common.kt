package org.http4k.openapi

import com.squareup.kotlinpoet.FileSpec
import org.http4k.poet.Property
import java.io.File

data class GenerationOptions(private val basePackage: String, val destinationFolder: File) {
    fun packageName(name: String) = "$basePackage.$name"
}

interface ApiGenerator : (OpenApi3Spec, GenerationOptions) -> List<FileSpec>

val httpHandler = Property("org.http4k.core.HttpHandler")
