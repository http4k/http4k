package org.http4k.storyboard.layout.slideshow

data class TileView(
    val index: Int,
    val title: String,
    val level: String,
    val boundary: ChapterBoundaryView? = null
)
