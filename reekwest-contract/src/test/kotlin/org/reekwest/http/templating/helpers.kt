package org.reekwest.http.templating

object NonExistent : View {
    override val template = "bibble"
}

data class OnClasspath(val items: List<Item>) : View {
    override val template = javaClass.name.replace('.', '/')
}

data class AtRoot(val items: List<Item>) : View {
    override val template = "AtRootBob"
}

data class Feature(val description: String)

data class Item(val name: String, val price: String, val features: List<Feature>)
