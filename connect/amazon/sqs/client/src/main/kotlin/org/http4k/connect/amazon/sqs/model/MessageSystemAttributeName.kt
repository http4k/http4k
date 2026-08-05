package org.http4k.connect.amazon.sqs.model

/** Selectable via ReceiveMessage's `MessageSystemAttributeNames`. [All] is a wildcard, not an attribute. */
object MessageSystemAttributeName {
    const val All = "All"
    const val ApproximateFirstReceiveTimestamp = "ApproximateFirstReceiveTimestamp"
    const val ApproximateReceiveCount = "ApproximateReceiveCount"
    const val AWSTraceHeader = "AWSTraceHeader"
    const val DeadLetterQueueSourceArn = "DeadLetterQueueSourceArn"
    const val MessageDeduplicationId = "MessageDeduplicationId"
    const val MessageGroupId = "MessageGroupId"
    const val SenderId = "SenderId"
    const val SentTimestamp = "SentTimestamp"
    const val SequenceNumber = "SequenceNumber"
}
