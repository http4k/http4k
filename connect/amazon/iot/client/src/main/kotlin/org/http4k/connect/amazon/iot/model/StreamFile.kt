package org.http4k.connect.amazon.iot.model

import se.ansman.kotshi.JsonSerializable

/**
 * One file in a stream, addressed by a [fileId] of 0 to 255 which the device names when it
 * asks for blocks. A stream carries 1 to 50 of these.
 */
@JsonSerializable
data class StreamFile(
    val fileId: Int,
    val s3Location: S3Location,
)

/** Where the file's bytes live. [version] pins an object version on a versioned bucket. */
@JsonSerializable
data class S3Location(
    val bucket: String,
    val key: String,
    val version: String? = null,
)
