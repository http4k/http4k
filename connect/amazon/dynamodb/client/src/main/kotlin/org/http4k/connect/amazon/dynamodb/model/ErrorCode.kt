package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
enum class ErrorCode {
    ConditionalCheckFailed,
    ItemCollectionSizeLimitExceeded,
    RequestLimitExceeded,
    ValidationError,
    ProvisionedThroughputExceeded,
    TransactionConflict,
    ThrottlingError,
    InternalServerError,
    ResourceNotFound,
    AccessDenied,
    DuplicateItem
}
