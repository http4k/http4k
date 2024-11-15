package org.http4k.connect.amazon.kms.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.KMSKeyId
import org.http4k.connect.amazon.kms.KMSAction
import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class ScheduleKeyDeletion(val KeyId: KMSKeyId, val PendingWindowInDays: Int? = null) :
    KMSAction<KeyDeletionSchedule>(KeyDeletionSchedule::class)

@JsonSerializable
data class KeyDeletionSchedule(val KeyId: KMSKeyId, val DeletionDate: Timestamp)
