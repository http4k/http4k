package org.reekwest.http.formats

import argo.format.CompactJsonFormatter
import argo.format.PrettyJsonFormatter
import argo.jdom.JdomParser
import argo.jdom.JsonNode
import argo.jdom.JsonNodeFactories
import argo.jdom.JsonNodeFactories.`object`
import argo.jdom.JsonNodeFactories.array
import argo.jdom.JsonNodeFactories.booleanNode
import argo.jdom.JsonNodeFactories.nullNode
import argo.jdom.JsonNodeFactories.number
import argo.jdom.JsonNodeFactories.string
import argo.jdom.JsonRootNode
import java.math.BigDecimal
import java.math.BigInteger

object Argo : Json<JsonRootNode, JsonNode> {

    private val pretty = PrettyJsonFormatter()
    private val compact = CompactJsonFormatter()
    private val jdomParser = JdomParser()

    override fun String.fromJsonString(): JsonRootNode = this.let(jdomParser::parse)
    override fun String?.asJsonValue(): JsonNode = this?.let { string(it) } ?: nullNode()
    override fun Int?.asJsonValue(): JsonNode = this?.let { number(it.toLong()) } ?: nullNode()
    override fun Double?.asJsonValue(): JsonNode = this?.let { number(BigDecimal(it)) } ?: nullNode()
    override fun Long?.asJsonValue(): JsonNode = this?.let { number(it) } ?: nullNode()
    override fun BigDecimal?.asJsonValue(): JsonNode = this?.let { number(it) } ?: nullNode()
    override fun BigInteger?.asJsonValue(): JsonNode = this?.let { number(it) } ?: nullNode()
    override fun Boolean?.asJsonValue(): JsonNode = this?.let { booleanNode(it) } ?: nullNode()
    override fun <T : Iterable<JsonNode>> T.asJsonArray(): JsonRootNode = array(this)
    override fun JsonRootNode.asPrettyJsonString(): String = pretty.format(this)
    override fun JsonRootNode.asCompactJsonString(): String = compact.format(this)
    override fun <LIST : Iterable<Pair<String, JsonNode>>> LIST.asJsonObject(): JsonRootNode = `object`(this.map { field(it.first, it.second) })

    private fun field(name: String, value: JsonNode) = JsonNodeFactories.field(name, value)
}
