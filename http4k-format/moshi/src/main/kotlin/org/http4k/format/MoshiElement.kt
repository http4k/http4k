package org.http4k.format

sealed interface MoshiElement

data class MoshiArray(val elements: List<MoshiElement>): MoshiElement
data class MoshiObject(val attributes: Map<String, MoshiElement>): MoshiElement
sealed interface MoshiPrimitive: MoshiElement
data class MoshiString(val value: String): MoshiPrimitive
data class MoshiNumber(val value: Number): MoshiPrimitive
data class MoshiBoolean(val value: Boolean): MoshiPrimitive
object MoshiNull: MoshiPrimitive
