package org.http4k.openapi.v3

import com.squareup.kotlinpoet.FileSpec
import java.io.File

data class GenerationOptions(private val basePackage: String, val destinationFolder: File) {
    fun packageName(name: String) = "$basePackage.$name"
}

interface ApiGenerator : (OpenApi3Spec, GenerationOptions) -> List<FileSpec>
