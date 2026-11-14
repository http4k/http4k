package org.http4k.connect.openfga.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class UserTypeFilter(val type: TypeName, val relation: Relation? = null)
