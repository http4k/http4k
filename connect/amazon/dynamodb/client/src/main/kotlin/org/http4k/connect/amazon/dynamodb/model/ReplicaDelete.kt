package org.http4k.connect.amazon.dynamodb.model

import org.http4k.connect.amazon.core.model.Region
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ReplicaDelete(val RegionName: Region?)
