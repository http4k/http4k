package org.http4k.template

class PebbleTemplatesTest : TemplatesContract<PebbleTemplates>(PebbleTemplates())

class PebbleViewModelTest : ViewModelContract(PebbleTemplates())
