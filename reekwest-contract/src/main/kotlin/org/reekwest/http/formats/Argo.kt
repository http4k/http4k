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
import org.reekwest.http.lens.BiDiLensSpec
import org.reekwest.http.lens.Body
import java.math.BigDecimal
import java.math.BigInteger

object Argo : Json<JsonRootNode, JsonNode> {

    private val pretty = PrettyJsonFormatter()
    private val compact = CompactJsonFormatter()
    private val jdomParser = JdomParser()

    override fun String.fromJsonString() = this.let(jdomParser::parse)
    override fun String?.asJson() = this?.let { string(it) } ?: nullNode()
    override fun Int?.asJson() = this?.let { number(it.toLong()) } ?: nullNode()
    override fun Double?.asJson() = this?.let { number(BigDecimal(it)) } ?: nullNode()
    override fun Long?.asJson() = this?.let { number(it) } ?: nullNode()
    override fun BigDecimal?.asJson() = this?.let { number(it) } ?: nullNode()
    override fun BigInteger?.asJson() = this?.let { number(it) } ?: nullNode()
    override fun Boolean?.asJson() = this?.let { booleanNode(it) } ?: nullNode()
    override fun <T : Iterable<JsonNode>> T.asJsonArray() = array(this)
    override fun JsonRootNode.asPrettyJsonString() = pretty.format(this)
    override fun JsonRootNode.asCompactJsonString() = compact.format(this)
    override fun <LIST : Iterable<Pair<String, JsonNode>>> LIST.asJsonObject() = `object`(this.map { field(it.first, it.second) })
    override fun <IN> BiDiLensSpec<IN, String, String>.json() = this.map(Argo::parse, Argo::compact)
    override fun Body.json() = string.map(Argo::parse, Argo::compact)

    private fun field(name: String, value: JsonNode) = JsonNodeFactories.field(name, value)
}
