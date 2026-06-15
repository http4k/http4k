package org.http4k.storyboard.theme.flat

data class SectionView(
    val title: String,
    val depth: Int,
    val headingLevel: String,
    val frames: List<FlatFrameView>,
    val children: List<SectionView>
)
