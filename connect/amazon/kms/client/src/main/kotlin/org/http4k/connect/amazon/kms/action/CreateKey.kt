package org.http4k.connect.amazon.kms.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.amazon.kms.KMSAction
import org.http4k.connect.amazon.kms.model.CustomerMasterKeySpec
import org.http4k.connect.amazon.kms.model.KeyMetadata
import org.http4k.connect.amazon.kms.model.KeyUsage
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class CreateKey(
    val KeySpec: CustomerMasterKeySpec? = null,
    val KeyUsage: KeyUsage? = null,
    val BypassPolicyLockoutSafetyCheck: Boolean? = null,
    val CustomKeyStoreId: String? = null,
    val Description: String? = null,
    val Origin: String? = null,
    val Policy: String? = null,
    val Tags: List<Tag>? = null
) : KMSAction<KeyCreated>(KeyCreated::class)

@JsonSerializable
data class KeyCreated(val KeyMetadata: KeyMetadata)
