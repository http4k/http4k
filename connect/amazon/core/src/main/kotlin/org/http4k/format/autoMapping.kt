package org.http4k.format

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AccessKeyId
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.connect.amazon.core.model.Expiration
import org.http4k.connect.amazon.core.model.KMSKeyId
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.core.model.RoleSessionName
import org.http4k.connect.amazon.core.model.SecretAccessKey
import org.http4k.connect.amazon.core.model.SessionToken
import org.http4k.connect.amazon.core.model.WebIdentityToken
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.Timestamp
import org.http4k.connect.model.TimestampMillis
import org.http4k.lens.BiDiMapping

fun <T> AutoMappingConfiguration<T>.withAwsCoreMappings() = apply {
    value(AccessKeyId)
    value(ARN)
    value(AwsService)
    value(AwsAccount)
    value(Base64Blob)
    value(Expiration)
    value(KMSKeyId)
    value(Region)
    value(RoleSessionName)
    value(SecretAccessKey)
    value(SessionToken)
    value(WebIdentityToken)
    double(BiDiMapping({ Timestamp.of(it.toLong()) }, { it.value.toDouble() }))
    value(TimestampMillis)
}
