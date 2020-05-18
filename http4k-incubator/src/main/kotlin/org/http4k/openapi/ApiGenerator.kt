package org.http4k.openapi

import com.squareup.kotlinpoet.FileSpec

interface ApiGenerator<T> : (T, GenerationOptions) -> List<FileSpec>
