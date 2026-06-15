package org.http4k.storyboard.renderer.slideshow

import org.http4k.storyboard.EndToEndContract
import org.http4k.storyboard.theme.acmeTheme
import org.http4k.storyboard.theme.slideshow.Slideshow

class EndToEndSlideshowAcmeTest : EndToEndContract(Slideshow(acmeTheme))
