/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

/**
 * Visual customisation for the built-in layouts.
 */
data class Theme(
    val brandName: String,
    val brandHref: String,
    val logoUrl: String,
    val faviconLarge: String,
    val faviconSmall: String,
    val headerBackground: String,
    val accentColor: String,
    val linkColor: String,
    val textColor: String,
    val textMutedColor: String,
    val borderColor: String,
    val bgLight: String,
    val bgHover: String,
    val extraHeadHtml: String,
) {
    companion object {
        val Http4k = Theme(
            brandName = "Storyboard",
            brandHref = "https://http4k.org",
            logoUrl = "https://http4k.org/images/logo.png",
            faviconLarge = "https://http4k.org/favicon-32.png",
            faviconSmall = "https://http4k.org/favicon-16.png",
            headerBackground = """radial-gradient(circle at 82% -40%, rgba(97, 192, 255, 0.10), transparent 60%), url("https://http4k.org/images/pipes-hero.svg") no-repeat left top / cover, #0B1622""",
            accentColor = "#EE7D2B",
            linkColor = "#0f6cb8",
            textColor = "#1d2433",
            textMutedColor = "#5a6577",
            borderColor = "#E2E8F1",
            bgLight = "#F5F8FC",
            bgHover = "#E9EEF5",
            extraHeadHtml = ""
        )
    }
}
