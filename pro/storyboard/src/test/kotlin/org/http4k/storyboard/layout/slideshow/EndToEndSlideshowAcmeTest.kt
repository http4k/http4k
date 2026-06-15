package org.http4k.storyboard.layout.slideshow

import org.http4k.storyboard.EndToEndContract
import org.http4k.storyboard.theme.acmeTheme

class EndToEndSlideshowAcmeTest : EndToEndContract(Slideshow(acmeTheme))
