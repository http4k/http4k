package org.http4k.connect.amazon.sqs.model

import org.http4k.connect.amazon.core.model.DataType
import org.http4k.connect.amazon.core.model.MessageFields
import org.http4k.connect.model.Base64Blob

interface MessageSystemAttribute : MessageFields

fun MessageSystemAttribute(name: String, value: String, dataType: DataType): MessageSystemAttribute =
    object : MessageSystemAttribute,
        MessageFields by SQSMessageAttribute.SingularValue(name, "MessageSystemAttribute", "String", value, dataType) {}
