/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

/**
 * Visual customisation for the built-in renderers.
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
            headerBackground = """url("https://http4k.org/images/pipes-hero.svg"), linear-gradient(135deg, #59AFF5 0%, #2096F3 50%, #61C0FF 100%)""",
            accentColor = "#fd7e14",
            linkColor = "#0d6efd",
            textColor = "#212529",
            textMutedColor = "#6c757d",
            borderColor = "#dee2e6",
            bgLight = "#f8f9fa",
            bgHover = "#e9ecef",
            extraHeadHtml = ""
        )
    }
}
