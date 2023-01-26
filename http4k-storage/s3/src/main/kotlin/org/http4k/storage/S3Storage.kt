package org.http4k.storage

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import org.http4k.connect.amazon.s3.S3Bucket
import org.http4k.connect.amazon.s3.action.DeleteObject
import org.http4k.connect.amazon.s3.action.ListObjectsV2
import org.http4k.connect.amazon.s3.model.BucketKey
import org.http4k.format.AutoMarshalling
import org.http4k.format.Moshi

/**
 * S3-backed storage implementation. Automatically marshals objects to and from string-value format.
 */
inline fun <reified T : Any> Storage.Companion.S3(s3: S3Bucket, autoMarshalling: AutoMarshalling = Moshi): Storage<T> =
    object : Storage<T> {
        override fun get(key: String): T? = s3[BucketKey.of(key)]
            .map { it?.reader()?.readText()?.let { autoMarshalling.asA<T>(it) } }
            .recover { it.throwIt() }

        override fun set(key: String, data: T) {
            s3[BucketKey.of(key)] = autoMarshalling.asInputStream(data)
        }

        override fun remove(key: String) =
            s3(DeleteObject(BucketKey.of(key)))
                .map { true }
                .recover { it.throwIt() }

        override fun keySet(keyPrefix: String) =
            when (val result = s3(ListObjectsV2())) {
                is Success -> result.value.items
                    .filter { it.Key.value.startsWith(keyPrefix) }
                    .map { it.Key.value }
                    .toSet()
                is Failure -> result.reason.throwIt()
            }

        override fun removeAll(keyPrefix: String) = with(keySet(keyPrefix).map { BucketKey.of(it) }) {
            when {
                isEmpty() -> false
                else -> {
                    forEach { s3(DeleteObject(it)) }
                    true
                }
            }
        }
    }
