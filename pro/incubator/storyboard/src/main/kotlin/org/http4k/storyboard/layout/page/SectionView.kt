package org.http4k.storyboard.layout.page

data class SectionView(
    val title: String,
    val depth: Int,
    val headingLevel: String,
    val frames: List<PageFrameView>,
    val children: List<SectionView>
)
