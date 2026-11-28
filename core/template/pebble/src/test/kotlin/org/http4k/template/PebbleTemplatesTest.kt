package org.http4k.template

class PebbleTemplatesTest : TemplatesContract<PebbleTemplates>(PebbleTemplates(allowList("org.http4k")))

class PebbleViewModelTest : ViewModelContract(PebbleTemplates(allowList("org.http4k")))
