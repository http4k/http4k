package org.http4k.template

import org.http4k.templates.ViewModel

object NonExistent : ViewModel {
    override fun template() = "bibble"
}

data class OnClasspath(val items: List<Item>) : ViewModel {
    override fun template() = javaClass.name.replace('.', '/')
}

data class AtRoot(val items: List<Item>) : ViewModel {
    override fun template() = "AtRootBob"
}

data class Feature(val description: String)

data class Item(val name: String, val price: String, val features: List<Feature>)
