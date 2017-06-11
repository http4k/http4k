package org.http4k.template

class PebbleTemplatesTest : TemplatesContract(PebbleTemplates())

class PebbleViewModelTest : ViewModelContract(PebbleTemplates())