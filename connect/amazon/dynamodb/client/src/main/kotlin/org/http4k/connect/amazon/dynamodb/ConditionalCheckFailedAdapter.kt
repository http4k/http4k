package org.http4k.connect.amazon.dynamodb

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.http4k.connect.amazon.dynamodb.action.ConditionalCheckFailed
import java.lang.reflect.Type

private const val AWS_NAME = "message"
private const val DYNAMODB_LOCAL_NAME = "Message"

/**
 * DynamoDB names the error text `message`, but DynamoDB Local names it `Message`. The AWS SDKs
 * accept either, so reads here do too - otherwise a failed conditional write cannot be parsed at
 * all against one of the two. Writes always use DynamoDB's own spelling.
 */
object ConditionalCheckFailedAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (Types.getRawType(type) != ConditionalCheckFailed::class.java) return null

        val delegate = DynamoDbJsonAdapterFactory.create(type, annotations, moshi)
            ?.let { @Suppress("UNCHECKED_CAST") (it as JsonAdapter<ConditionalCheckFailed>) }
            ?: return null

        return object : JsonAdapter<ConditionalCheckFailed>() {
            override fun fromJson(reader: JsonReader) = delegate.fromJsonValue(
                when (val value = reader.readJsonValue()) {
                    is Map<*, *> -> value.mapKeys { (key, _) ->
                        if (key == DYNAMODB_LOCAL_NAME) AWS_NAME else key
                    }

                    else -> throw JsonDataException("Expected a ConditionalCheckFailed object")
                }
            )

            override fun toJson(writer: JsonWriter, value: ConditionalCheckFailed?) =
                delegate.toJson(writer, value)
        }.nullSafe()
    }
}
