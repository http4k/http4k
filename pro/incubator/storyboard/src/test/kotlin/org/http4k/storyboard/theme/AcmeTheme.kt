/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.theme

import org.http4k.storyboard.Theme

internal val acmeTheme = Theme(
    brandName = "Acme Docs",
    brandHref = "https://acme.example.com",
    logoUrl = "https://static.wikia.nocookie.net/fictionalcompanies/images/c/c2/ACME_Corporation.png/revision/latest",
    faviconLarge = "https://http4k.org/favicon-16.png",
    faviconSmall = "https://http4k.org/favicon-32.png",
    headerBackground = "linear-gradient(135deg, #1a2a6c 0%, #b21f1f 50%, #fdbb2d 100%)",
    accentColor = "#00b894",
    linkColor = "#0984e3",
    textColor = "#2d3436",
    textMutedColor = "#636e72",
    borderColor = "#dfe6e9",
    bgLight = "#f6f8fa",
    bgHover = "#e0e6eb",
    extraHeadHtml = "<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">"
)
