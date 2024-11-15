/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.http4k.connect.amazon.sqs

import org.http4k.asByteBuffer
import org.http4k.connect.amazon.core.model.DataType
import org.http4k.connect.amazon.sqs.model.MessageAttribute
import org.http4k.util.Hex.hex
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * From the AWS SDK
 */
object MessageMD5ChecksumInterceptor {
    private const val INTEGER_SIZE_IN_BYTES = 4
    private const val STRING_TYPE_FIELD_INDEX: Byte = 1
    private const val BINARY_TYPE_FIELD_INDEX: Byte = 2
    private const val STRING_LIST_TYPE_FIELD_INDEX: Byte = 3
    private const val BINARY_LIST_TYPE_FIELD_INDEX: Byte = 4

    fun calculateMd5(attrList: List<MessageAttribute>): String {
        val md5Digest = MessageDigest.getInstance("MD5")

        val attrs = attrList.associateBy { it.name }
        val sortedAttributeNames = attrs.keys.sorted()

        for (attrName in sortedAttributeNames) {
            val attrValue = attrs[attrName]
            updateLengthAndBytes(md5Digest, attrName)
            updateLengthAndBytes(md5Digest, attrValue!!.dataType.name)

            if (attrValue.dataType == DataType.String) {
                when {
                    attrValue.value.contains("~http4k~") -> {
                        md5Digest.update(STRING_LIST_TYPE_FIELD_INDEX)
                        for (strListMember in attrValue.value.split("~http4k~").filterNot { it == "" }) {
                            updateLengthAndBytes(md5Digest, strListMember);
                        }
                    }

                    else -> {
                        md5Digest.update(STRING_TYPE_FIELD_INDEX);
                        updateLengthAndBytes(md5Digest, attrValue.value);
                    }
                }
            } else if (attrValue.dataType == DataType.Binary) {
                when {
                    attrValue.value.contains("~http4k~") -> {
                        md5Digest.update(BINARY_LIST_TYPE_FIELD_INDEX)
                        for (strListMember in attrValue.value.split("~http4k~").filterNot { it == "" }) {
                            updateLengthAndBytes(md5Digest, strListMember.asByteBuffer());
                        }
                    }

                    else -> {
                        md5Digest.update(BINARY_TYPE_FIELD_INDEX);
                        updateLengthAndBytes(md5Digest, attrValue.value.asByteBuffer());
                    }
                }
            }
        }
        return hex(md5Digest.digest())
    }

    private fun updateLengthAndBytes(digest: MessageDigest, str: String) {
        val utf8Encoded = str.toByteArray(StandardCharsets.UTF_8)
        digest.update(ByteBuffer.allocate(INTEGER_SIZE_IN_BYTES).putInt(utf8Encoded.size).array())
        digest.update(utf8Encoded)
    }

    private fun updateLengthAndBytes(digest: MessageDigest, binaryValue: ByteBuffer) {
        val readOnlyBuffer = binaryValue.asReadOnlyBuffer()
        val size = readOnlyBuffer.remaining()
        val lengthBytes = ByteBuffer.allocate(INTEGER_SIZE_IN_BYTES).putInt(size)
        digest.update(lengthBytes.array())
        digest.update(readOnlyBuffer)
    }
}
