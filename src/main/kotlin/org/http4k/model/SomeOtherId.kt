package org.http4k.model

import kotlin.Any
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

data class SomeOtherId(
	val string: String,
	val child: ArbObject1?,
	val numbers: List<Map<String, Any>>,
	val bool: Boolean
)
