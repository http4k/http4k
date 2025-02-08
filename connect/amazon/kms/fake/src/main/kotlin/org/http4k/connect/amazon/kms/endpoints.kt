package org.http4k.connect.amazon.kms

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.connect.amazon.core.model.KMSKeyId
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.kms.KMSSigningAlgorithm.Companion.KMS_ALGORITHMS
import org.http4k.connect.amazon.kms.action.CreateKey
import org.http4k.connect.amazon.kms.action.Decrypt
import org.http4k.connect.amazon.kms.action.Decrypted
import org.http4k.connect.amazon.kms.action.DescribeKey
import org.http4k.connect.amazon.kms.action.Encrypt
import org.http4k.connect.amazon.kms.action.Encrypted
import org.http4k.connect.amazon.kms.action.GetPublicKey
import org.http4k.connect.amazon.kms.action.KeyCreated
import org.http4k.connect.amazon.kms.action.KeyDeletionSchedule
import org.http4k.connect.amazon.kms.action.KeyDescription
import org.http4k.connect.amazon.kms.action.KeyList
import org.http4k.connect.amazon.kms.action.ListKeys
import org.http4k.connect.amazon.kms.action.PublicKey
import org.http4k.connect.amazon.kms.action.ScheduleKeyDeletion
import org.http4k.connect.amazon.kms.action.Sign
import org.http4k.connect.amazon.kms.action.Signed
import org.http4k.connect.amazon.kms.action.Verify
import org.http4k.connect.amazon.kms.action.VerifyResult
import org.http4k.connect.amazon.kms.model.CustomerMasterKeySpec
import org.http4k.connect.amazon.kms.model.CustomerMasterKeySpec.SYMMETRIC_DEFAULT
import org.http4k.connect.amazon.kms.model.EncryptionAlgorithm
import org.http4k.connect.amazon.kms.model.KeyEntry
import org.http4k.connect.amazon.kms.model.KeyMetadata
import org.http4k.connect.amazon.kms.model.KeyUsage
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.Timestamp
import org.http4k.connect.storage.Storage
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Provider
import java.util.UUID

@Suppress("DEPRECATION")
fun AwsJsonFake.createKey(keys: Storage<StoredCMK>, crypto: Provider) = route<CreateKey> {
    val keyId = KMSKeyId.of(UUID.randomUUID().toString())
    val keySpec = it.KeySpec ?: SYMMETRIC_DEFAULT

    val keyPair = when (keySpec) {
        CustomerMasterKeySpec.RSA_2048 -> KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
        CustomerMasterKeySpec.RSA_3072 -> KeyPairGenerator.getInstance("RSA").apply { initialize(3072) }
        CustomerMasterKeySpec.RSA_4096 -> KeyPairGenerator.getInstance("RSA").apply { initialize(4096) }
        CustomerMasterKeySpec.ECC_NIST_P256 -> KeyPairGenerator.getInstance("ECDSA", crypto).apply { initialize(256) }
        CustomerMasterKeySpec.ECC_NIST_P384 -> KeyPairGenerator.getInstance("ECDSA", crypto).apply { initialize(384) }
        CustomerMasterKeySpec.ECC_NIST_P521 -> KeyPairGenerator.getInstance("ECDSA", crypto).apply { initialize(521) }
        CustomerMasterKeySpec.ECC_SECG_P256K1 -> null
        SYMMETRIC_DEFAULT -> null
    }?.generateKeyPair()

    val storedCMK = StoredCMK(
        keyId = keyId,
        arn = keyId.toArn(),
        keyUsage = it.KeyUsage ?: KeyUsage.ENCRYPT_DECRYPT,
        keySpec = keySpec,
        publicKeyContent = keyPair?.public?.let { key ->
            EncryptionKeyContent(key.format, Base64Blob.encode(key.encoded))
        },
        privateKeyContent = keyPair?.private?.let { key ->
            EncryptionKeyContent(key.format, Base64Blob.encode(key.encoded))
        }
    )

    keys[storedCMK.arn.value] = storedCMK

    KeyCreated(KeyMetadata(storedCMK.keyId, storedCMK.arn, AwsAccount.of("0"), it.KeyUsage))
}

fun AwsJsonFake.listKeys(keys: Storage<StoredCMK>) = route<ListKeys> {
    KeyList(
        keys.keySet("")
            .mapNotNull { keys[it] }
            .map { KeyEntry(it.keyId, it.arn) }
    )
}

fun AwsJsonFake.describeKey(keys: Storage<StoredCMK>) = route<DescribeKey> { req ->
    keys[req.KeyId.toArn().value]?.let {
        KeyDescription(KeyMetadata(it.keyId, it.arn, AwsAccount.of("0"), it.keyUsage))
    }
}

fun AwsJsonFake.decrypt(keys: Storage<StoredCMK>) = route<Decrypt> { req ->
    keys[req.KeyId.toArn().value]?.let {
        val plainText = Base64Blob.encode(req.CiphertextBlob.decodedBytes().reversed().toByteArray())
        Decrypted(KMSKeyId.of(it.arn), plainText, req.EncryptionAlgorithm ?: EncryptionAlgorithm.SYMMETRIC_DEFAULT)
    }
}

fun AwsJsonFake.encrypt(keys: Storage<StoredCMK>) = route<Encrypt> { req ->
    keys[req.KeyId.toArn().value]?.let {
        Encrypted(
            KMSKeyId.of(it.arn),
            Base64Blob.encode(req.Plaintext.decodedBytes().reversed().toByteArray()),
            req.EncryptionAlgorithm
                ?: EncryptionAlgorithm.SYMMETRIC_DEFAULT
        )
    }
}

fun AwsJsonFake.getPublicKey(keys: Storage<StoredCMK>) = route<GetPublicKey> {
    keys[it.KeyId.toArn().value]?.let { cmk ->
        PublicKey(
            KMSKeyId.of(cmk.arn), cmk.keySpec, cmk.keyUsage,
            cmk.publicKeyContent!!.encoded, null,
            cmk.signingAlgorithms
        )
    }
}

fun AwsJsonFake.scheduleKeyDeletion(keys: Storage<StoredCMK>) = route<ScheduleKeyDeletion> { req ->
    keys[req.KeyId.toArn().value]?.let {
        keys[req.KeyId.toArn().value] = it.copy(deletion = Timestamp.of(Long.MAX_VALUE))
        KeyDeletionSchedule(KMSKeyId.of(it.arn), Timestamp.of(Long.MAX_VALUE))
    }
}

fun AwsJsonFake.sign(keys: Storage<StoredCMK>, crypto: Provider) = route<Sign> { req ->
    keys[req.KeyId.toArn().value]?.let {
        Signed(
            KMSKeyId.of(it.arn),
            signTheBytes(req, it.loadPrivateKey(crypto)!!), req.SigningAlgorithm
        )
    }
}

private fun signTheBytes(req: Sign, key: PrivateKey) =
    KMS_ALGORITHMS[req.SigningAlgorithm]?.sign(key, req.Message)
        ?: Base64Blob.encode(req.SigningAlgorithm.name + req.Message.decoded().take(50))

fun AwsJsonFake.verify(keys: Storage<StoredCMK>, crypto: Provider) =
    route<Verify>(
        {
            when ((it as? VerifyResult)?.SignatureValid) {
                true -> Response(OK).body(autoMarshalling.asFormatString(it))
                else -> Response(BAD_REQUEST).body("""{"__type":"KMSInvalidSignatureException"}""")
            }
        }
    ) { req ->
        keys[req.KeyId.toArn().value]?.let {
            VerifyResult(req.KeyId, verifyTheBytes(req, it.loadPublicKey(crypto)!!), req.SigningAlgorithm)
        }
    }

private fun verifyTheBytes(req: Verify, key: java.security.PublicKey): Boolean {
    val alg = KMS_ALGORITHMS[req.SigningAlgorithm]
    return alg
        ?.verify(key, req.Message, req.Signature)
        ?: req.Signature.decoded().startsWith(req.SigningAlgorithm.name)
}

fun KMSKeyId.toArn() = when {
    value.startsWith("arn") -> ARN.of(value)
    else -> ARN.of(AwsService.of("kms"), Region.of("ldn-north-1"), AwsAccount.of("0"), "key", this)
}
