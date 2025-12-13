package org.http4k.format

import dev.toonformat.jtoon.DecodeOptions
import dev.toonformat.jtoon.EncodeOptions

object Toon : ConfigurableToon(ToonBuilder(), EncodeOptions.DEFAULT, DecodeOptions.DEFAULT)
