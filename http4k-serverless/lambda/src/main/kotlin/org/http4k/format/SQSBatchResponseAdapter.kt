package org.http4k.format

import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse.BatchItemFailure
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

object SQSBatchResponseAdapter : JsonAdapter<SQSBatchResponse>() {
    override fun fromJson(reader: JsonReader): SQSBatchResponse =
        with(reader) {
            obj(::SQSBatchResponse) {
                when (it) {
                    "batchItemFailures" -> batchItemFailures = list(::BatchItemFailure) {
                        when (it) {
                            "itemIdentifier" -> itemIdentifier = nextString()
                        }
                    }
                }
            }
        }

    override fun toJson(writer: JsonWriter, response: SQSBatchResponse?) {
        with(writer) {
            obj(response) {
                list("batchItemFailures", batchItemFailures) {
                    obj(this) {
                        string("itemIdentifier", itemIdentifier)
                    }
                }
            }
        }
    }
}
