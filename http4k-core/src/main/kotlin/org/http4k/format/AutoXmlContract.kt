package org.http4k.format

abstract class AutoXmlContract(val x: AutoMarshallingXml) {
    val xml = """
<Base><xml><subWithText><attr>attr1</attr><content>content1</content></subWithText><subWithText><attr>attr2</attr><content>content2</content></subWithText><subWithAttr><attr>attr3</attr></subWithAttr><content>content3</content></xml></Base>
""".trimMargin()

    abstract fun `roundtrip xml to and from object`()
}
