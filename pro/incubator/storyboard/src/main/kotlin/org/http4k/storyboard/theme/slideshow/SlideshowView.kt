/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.theme.slideshow

import org.http4k.storyboard.Theme
import org.http4k.storyboard.util.StoryboardViewModel

data class SlideshowView(
    val theme: Theme,
    val pageTitle: String,
    val heading: String,
    val series: String?,
    val tiles: List<TileView>,
    val framesJson: String,
    val chapterPathsJson: String,
    val defaultMode: String
) : StoryboardViewModel

