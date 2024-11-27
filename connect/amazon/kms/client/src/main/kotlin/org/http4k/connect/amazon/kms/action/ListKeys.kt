package org.http4k.connect.amazon.kms.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.Paged
import org.http4k.connect.PagedAction
import org.http4k.connect.amazon.kms.KMSAction
import org.http4k.connect.amazon.kms.model.KeyEntry
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class ListKeys(
    val Limit: Int? = null,
    val Marker: String? = null
) : KMSAction<KeyList>(KeyList::class),
    PagedAction<String, KeyEntry, KeyList, ListKeys> {
    override fun next(token: String) = copy(Marker = token)
}

@JsonSerializable
data class KeyList(val Keys: List<KeyEntry>, val NextMarker: String? = null) : Paged<String, KeyEntry> {
    override fun token() = NextMarker
    override val items = Keys
}
