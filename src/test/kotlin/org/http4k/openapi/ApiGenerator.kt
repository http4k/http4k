package org.http4k.openapi

import com.squareup.kotlinpoet.FileSpec

interface ApiGenerator : (OpenApi3Spec) -> List<FileSpec>
