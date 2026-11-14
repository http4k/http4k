package org.http4k.connect.openfga.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Stores(val stores: List<Store>)
