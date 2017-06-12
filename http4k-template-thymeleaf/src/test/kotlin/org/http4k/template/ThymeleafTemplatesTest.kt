package org.http4k.template

class ThymeleafTemplatesTest : TemplatesContract(ThymeleafTemplates())

class ThymeleafViewModelTest : ViewModelContract(ThymeleafTemplates())