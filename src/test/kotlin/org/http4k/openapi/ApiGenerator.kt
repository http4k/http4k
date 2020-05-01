package org.http4k.openapi

import com.squareup.kotlinpoet.FileSpec
import org.http4k.openapi.server.GenerationOptions

interface ApiGenerator : (OpenApi3Spec, GenerationOptions) -> List<FileSpec>
