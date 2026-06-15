package org.http4k.storyboard.theme.slideshow

data class TileView(
    val index: Int,
    val title: String,
    val level: String,
    val boundary: ChapterBoundaryView? = null
)
