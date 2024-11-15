package org.http4k.connect.amazon.sns.model

import org.http4k.connect.amazon.core.model.DataType
import org.http4k.connect.amazon.core.model.MessageFields
import org.http4k.connect.model.Base64Blob

interface MessageAttribute : MessageFields

fun MessageAttribute(name: String, value: String, dataType: DataType): MessageAttribute =
    object : MessageAttribute,
        MessageFields by SNSMessageAttribute.SingularValue(name, "MessageAttributes", "String", value, dataType) {}

@JvmName("MessageAttributeStringList")
fun MessageAttribute(name: String, value: List<String>, dataType: DataType): MessageAttribute =
    object : MessageAttribute,
        MessageFields by SNSMessageAttribute.ListValue(value, "MessageAttributes", "StringList", name, dataType) {}

fun MessageAttribute(name: String, value: Base64Blob): MessageAttribute =
    object : MessageAttribute,
        MessageFields by SNSMessageAttribute.SingularValue(
            name, "MessageAttributes", "Binary", value.value,
            DataType.Binary
        ) {}

@JvmName("MessageAttributeBinaryList")
fun MessageAttribute(name: String, value: List<Base64Blob>): MessageAttribute =
    object : MessageAttribute,
        MessageFields by SNSMessageAttribute.ListValue(
            value.map { it.value },
            "MessageAttributes",
            "StringList",
            name,
            DataType.Binary
        ) {}


