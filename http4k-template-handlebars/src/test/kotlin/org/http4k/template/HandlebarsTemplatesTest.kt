package org.http4k.template

class HandlebarsTemplatesTest : TemplatesContract(HandlebarsTemplates())

class HandlebarsViewModelTest : ViewModelContract(HandlebarsTemplates())