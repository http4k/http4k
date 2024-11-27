package org.http4k.connect.amazon.s3

import org.http4k.connect.amazon.s3.model.StorageClass

// TODO suggest we make the Headers ext functions public
fun BucketKeyContent.header(name: String) = headers.find { it.first.equals(name, true) }?.second
fun BucketKeyContent.header(name: String, value: String) = copy(
    headers = headers + (name to value)
)
fun BucketKeyContent.removeHeader(name: String) = copy(
    headers = headers.filterNot { it.first.equals(name, true) }
)
fun BucketKeyContent.replaceHeader(name: String, value: String) = removeHeader(name).header(name, value)

fun BucketKeyContent.storageClass() = header("x-amz-storage-class")
    ?.let { StorageClass.valueOf(it) }
    ?: StorageClass.STANDARD

internal fun StorageClass.requiresRestore() = this == StorageClass.GLACIER || this == StorageClass.DEEP_ARCHIVE

fun BucketKeyContent.restoreReady() = header("x-amz-restore")?.contains("ongoing-request=\"false\"") == true
