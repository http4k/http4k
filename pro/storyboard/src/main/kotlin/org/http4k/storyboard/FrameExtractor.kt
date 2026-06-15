/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

/**
 * Inspects an Otel event and returns a [StoryFrame] if it
 * recognises the event as a frame, else `null`.
 */
typealias FrameExtractor = (EventContext) -> StoryFrame?

