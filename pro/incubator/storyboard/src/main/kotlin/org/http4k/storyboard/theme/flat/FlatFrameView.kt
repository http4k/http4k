package org.http4k.storyboard.theme.flat

data class FlatFrameView(
    val title: String,
    val notes: String,
    val type: String,
    val level: String,
    val dom: String,
    val language: String?,
    val source: String?
)
