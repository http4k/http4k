package org.http4k.connect.amazon.sqs

import org.http4k.connect.amazon.sqs.model.SQSMessage
import java.math.BigInteger
import java.security.MessageDigest

fun SQSMessage.md5OfBody() = body.md5()

fun String.md5() = BigInteger(
    1,
    MessageDigest.getInstance("MD5").digest(toByteArray())
).toString(16).padStart(32, '0')

fun SQSMessage.md5OfAttributes() = MessageMD5ChecksumInterceptor.calculateMd5(this.attributes)
