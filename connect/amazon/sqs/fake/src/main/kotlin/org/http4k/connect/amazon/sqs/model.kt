package org.http4k.connect.amazon.sqs

import org.http4k.connect.amazon.core.model.MessageFieldsDto
import org.http4k.connect.amazon.sqs.model.SQSMessage
import org.http4k.connect.amazon.sqs.model.SQSMessageId
import org.http4k.connect.amazon.sqs.model.toSqs
import java.math.BigInteger
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant

fun SQSMessage.md5OfBody() = body.md5()

fun String.md5() = BigInteger(
    1,
    MessageDigest.getInstance("MD5").digest(toByteArray())
).toString(16).padStart(32, '0')

fun SQSMessage.md5OfAttributes() = MessageMD5ChecksumInterceptor.calculateMd5(this.attributes)

/** A FIFO queue with ContentBasedDeduplication set derives the deduplication id from this. */
internal fun String.sha256() = BigInteger(
    1,
    MessageDigest.getInstance("SHA-256").digest(toByteArray())
).toString(16).padStart(64, '0')

/** The queue settings the fake honours. Recorded by CreateQueue, dropped by DeleteQueue. */
data class QueueConfig(val contentBasedDeduplication: Boolean = false)

/** Null when the map is empty, because SQS omits the field rather than sending an empty digest. */
fun Map<String, MessageFieldsDto>.md5OfFields() = takeIf { it.isNotEmpty() }
    ?.let { fields -> MessageMD5ChecksumInterceptor.calculateMd5(fields.map { (name, value) -> value.toSqs(name) }) }

/**
 * The checksums are not recorded here, because SQS digests the request it has just received even
 * when it deduplicates that request away.
 */
data class DeduplicationRecord(
    val messageId: SQSMessageId,
    val sequenceNumber: String?,
    val sentAt: Instant
)

val DeduplicationInterval: Duration = Duration.ofMinutes(5)

fun String.isFifoQueue() = endsWith(".fifo")
