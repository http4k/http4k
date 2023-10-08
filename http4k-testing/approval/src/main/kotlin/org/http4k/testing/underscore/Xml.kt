/*
 * The MIT License (MIT)
 *
 * Copyright 2015-2023 Valentyn Kolesnikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.http4k.testing.underscore

import org.http4k.testing.underscore.Base32.Companion.decode
import org.http4k.testing.underscore.Base32.Companion.encode
import org.http4k.testing.underscore.Base32.DecodingException
import org.w3c.dom.Node
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.IOException
import java.io.StringReader
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.BiFunction
import java.util.function.Function
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import kotlin.math.max

object Xml {
    private const val NULL = "null"
    private const val ELEMENT_TEXT = "element"
    private const val CDATA = "#cdata-section"
    private const val COMMENT = "#comment"
    private const val ENCODING = "#encoding"
    private const val STANDALONE = "#standalone"
    private const val OMITXMLDECLARATION = "#omit-xml-declaration"
    private const val YES = "yes"
    private const val TEXT = "#text"
    private const val NUMBER = "-number"
    private const val ELEMENT = "<$ELEMENT_TEXT>"
    private const val CLOSED_ELEMENT = "</$ELEMENT_TEXT>"
    private const val EMPTY_ELEMENT = ELEMENT + CLOSED_ELEMENT
    private const val NULL_TRUE = " $NULL=\"true\"/>"
    private const val NUMBER_TEXT = " number=\"true\""
    private const val NUMBER_TRUE = "$NUMBER_TEXT>"
    private const val ARRAY = "-array"
    private const val ARRAY_TRUE = " array=\"true\""
    private const val NULL_ELEMENT = "<$ELEMENT_TEXT$NULL_TRUE"
    private const val BOOLEAN = "-boolean"
    private const val TRUE = "true"
    private const val SELF_CLOSING = "-self-closing"
    private const val STRING = "-string"
    private const val NULL_ATTR = "-null"
    private const val EMPTY_ARRAY = "-empty-array"
    private const val QUOT = "&quot;"
    private const val XML_HEADER = "<?xml "
    private const val DOCTYPE_TEXT = "!DOCTYPE"
    private const val ROOT = "root"
    private const val DOCTYPE_HEADER = "<$DOCTYPE_TEXT "
    private val XML_UNESCAPE: MutableMap<String, String> = HashMap()
    private val DOCUMENT = Document.createDocument()

    init {
        XML_UNESCAPE[QUOT] = "\""
        XML_UNESCAPE["&amp;"] = "&"
        XML_UNESCAPE["&lt;"] = "<"
        XML_UNESCAPE["&gt;"] = ">"
        XML_UNESCAPE["&apos;"] = "'"
    }

    @JvmOverloads
    @JvmStatic
    fun toXml(collection: Collection<*>?, identStep: XmlStringBuilder.Step = XmlStringBuilder.Step.TWO_SPACES): String {
        val builder: XmlStringBuilder = XmlStringBuilderWithoutRoot(identStep, StandardCharsets.UTF_8.name(), "")
        writeArray(collection, builder, ARRAY_TRUE)
        return builder.toString()
    }

    @JvmOverloads
    @JvmStatic
    fun toXml(
        map: Map<*, *>?,
        identStep: XmlStringBuilder.Step = XmlStringBuilder.Step.TWO_SPACES,
        newRootName: String = ROOT,
        arrayTrue: ArrayTrue = ArrayTrue.ADD
    ): String {
        val builder: XmlStringBuilder
        val localMap: Map<*, *>?
        if (map != null && map.containsKey(ENCODING)) {
            localMap = (map as LinkedHashMap<*, *>).clone() as MutableMap<*, *>
            builder = checkStandalone(localMap.remove(ENCODING).toString(), identStep, localMap)
        } else if (map != null && map.containsKey(STANDALONE)) {
            localMap = (map as LinkedHashMap<*, *>).clone() as MutableMap<*, *>
            builder = XmlStringBuilderWithoutRoot(
                identStep,
                StandardCharsets.UTF_8.name(),
                " standalone=\""
                        + (if (YES == map[STANDALONE]) YES else "no")
                        + "\""
            )
            localMap.remove(STANDALONE)
        } else if (map != null && map.containsKey(OMITXMLDECLARATION)) {
            localMap = (map as LinkedHashMap<*, *>).clone() as MutableMap<*, *>
            builder = XmlStringBuilderWithoutHeader(identStep, 0)
            localMap.remove(OMITXMLDECLARATION)
        } else {
            builder = XmlStringBuilderWithoutRoot(identStep, StandardCharsets.UTF_8.name(), "")
            localMap = map
        }
        checkLocalMap(builder, localMap, newRootName, if (arrayTrue == ArrayTrue.ADD) ARRAY_TRUE else "")
        return builder.toString()
    }

    private fun checkLocalMap(
        builder: XmlStringBuilder,
        localMap: Map<*, *>?,
        newRootName: String,
        arrayTrue: String
    ) {
        val localMap2: Map<*, *>?
        if (localMap != null && localMap.containsKey(DOCTYPE_TEXT)) {
            localMap2 = (localMap as LinkedHashMap<*, *>).clone() as MutableMap<*, *>
            localMap2.remove(DOCTYPE_TEXT)
            builder.append(DOCTYPE_HEADER)
                .append(localMap[DOCTYPE_TEXT].toString())
                .append(">")
                .newLine()
        } else {
            localMap2 = localMap
        }
        if (localMap2 == null || localMap2.size != 1 || XmlValue.getMapKey(localMap2).startsWith("-")
            || XmlValue.getMapValue(localMap2) is List<*>
        ) {
            if (ROOT == XmlValue.getMapKey(localMap2)) {
                writeArray(XmlValue.getMapValue(localMap2) as List<*>?, builder, arrayTrue)
            } else {
                XmlObject.writeXml(
                    localMap2,
                    getRootName(localMap2, newRootName),
                    builder,
                    false,
                    LinkedHashSet(),
                    false,
                    arrayTrue
                )
            }
        } else {
            XmlObject.writeXml(
                localMap2,
                getRootName(localMap2, newRootName),
                builder,
                false,
                LinkedHashSet(),
                false,
                arrayTrue
            )
        }
    }

    private fun writeArray(
        collection: Collection<*>?, builder: XmlStringBuilder, arrayTrue: String
    ) {
        builder.append("<root")
        if (collection != null && collection.isEmpty()) {
            builder.append(" empty-array=\"true\"")
        }
        builder.append(">").incIdent()
        if (collection != null && !collection.isEmpty()) {
            builder.newLine()
        }
        XmlArray.writeXml(collection, null, builder, false, LinkedHashSet(), false, arrayTrue)
        if (collection != null && !collection.isEmpty()) {
            builder.newLine()
        }
        builder.append("</root>")
    }

    private fun checkStandalone(
        encoding: String, identStep: XmlStringBuilder.Step, localMap: MutableMap<*, *>?
    ): XmlStringBuilder {
        val builder: XmlStringBuilder
        if (localMap!!.containsKey(STANDALONE)) {
            builder = XmlStringBuilderWithoutRoot(
                identStep,
                encoding,
                " standalone=\""
                        + (if (YES == localMap[STANDALONE]) YES else "no")
                        + "\""
            )
            localMap.remove(STANDALONE)
        } else {
            builder = XmlStringBuilderWithoutRoot(identStep, encoding, "")
        }
        return builder
    }

    private fun getRootName(localMap: Map<*, *>?, newRootName: String): String? {
        var foundAttrs = 0
        var foundElements = 0
        var foundListElements = 0
        if (localMap != null) {
            for ((key, value) in localMap.entries) {
                if (key.toString().startsWith("-")) {
                    foundAttrs += 1
                } else if (!key.toString().startsWith(COMMENT)
                    && !key.toString().startsWith(CDATA)
                    && !key.toString().startsWith("?")
                ) {
                    if (value is List<*> && value.size > 1) {
                        foundListElements += 1
                    }
                    foundElements += 1
                }
            }
        }
        return if (foundAttrs == 0 && foundElements == 1 && foundListElements == 0) null else newRootName
    }

    private operator fun getValue(name: String?, value: Any?, fromType: FromType): Any? {
        val localValue: Any? = if (value is Map<*, *> && (value as Map<String?, Any?>).entries.size == 1) {
                val (key, value1) = (value as Map<String, Any?>).entries.iterator().next()
                if (TEXT == key || fromType == FromType.FOR_CONVERT && ELEMENT_TEXT == key) {
                    value1
                } else {
                    value
                }
            } else {
                value
            }
        return (if (localValue is String && name!!.startsWith("-")) XmlValue.unescape(localValue as String?) else localValue)
    }

    @JvmStatic
    fun stringToNumber(number: String): Any {
        val localValue: Any
        localValue = if (number.contains(".") || number.contains("e") || number.contains("E")) {
            if (number.length > 9
                || number.contains(".") && number.length - number.lastIndexOf('.') > 2
                && number[number.length - 1] == '0'
            ) {
                BigDecimal(number)
            } else {
                number.toDouble()
            }
        } else {
            if (number.length > 19) {
                BigInteger(number)
            } else {
                number.toLong()
            }
        }
        return localValue
    }

    private fun createMap(
        node: Node,
        elementMapper: BiFunction<Any, Set<String>, String?>,
        nodeMapper: Function<Any?, Any?>,
        attrMap: Map<String?, Any?>,
        uniqueIds: IntArray,
        source: String,
        sourceIndex: IntArray,
        namespaces: MutableSet<String>,
        fromType: FromType
    ): Any {
        val map = LinkedHashMap<String?, Any?>()
        map.putAll(attrMap)
        val nodeList = node.childNodes
        for (index in 0 until nodeList.length) {
            val currentNode = nodeList.item(index)
            val name: String = if (currentNode.nodeType == Node.PROCESSING_INSTRUCTION_NODE) {
                "?" + currentNode.nodeName
            } else {
                currentNode.nodeName
            }
            val value: Any?
            if (currentNode.nodeType == Node.ELEMENT_NODE) {
                sourceIndex[0] = source.indexOf("<$name", sourceIndex[0]) + name.length + 1
                value = addElement(
                    sourceIndex,
                    source,
                    elementMapper,
                    nodeMapper,
                    uniqueIds,
                    currentNode,
                    namespaces,
                    fromType
                )
            } else {
                if (COMMENT == name) {
                    sourceIndex[0] = source.indexOf("-->", sourceIndex[0]) + 3
                } else if (CDATA == name) {
                    sourceIndex[0] = source.indexOf("]]>", sourceIndex[0]) + 3
                }
                value = currentNode.textContent
            }
            if (TEXT == name && node.childNodes.length > 1 && value.toString().trim { it <= ' ' }.isEmpty()) {
                continue
            }
            if (currentNode.nodeType == Node.DOCUMENT_TYPE_NODE) {
                addNodeValue(
                    map,
                    DOCTYPE_TEXT,
                    getDoctypeValue(source),
                    elementMapper,
                    nodeMapper,
                    uniqueIds,
                    namespaces,
                    fromType
                )
            } else {
                addNodeValue(
                    map,
                    name,
                    value,
                    elementMapper,
                    nodeMapper,
                    uniqueIds,
                    namespaces,
                    fromType
                )
            }
        }
        return checkNumberAndBoolean(map, node.nodeName)
    }

    private fun checkNumberAndBoolean(map: MutableMap<String?, Any?>, name: String): Any {
        val localMap: MutableMap<String?, Any?>
        if (map.containsKey(NUMBER) && TRUE == map[NUMBER] && map.containsKey(TEXT)) {
            localMap = (map as LinkedHashMap<String?, Any?>).clone() as MutableMap<String?, Any?>
            localMap.remove(NUMBER)
            localMap[TEXT] = stringToNumber(localMap[TEXT].toString())
        } else {
            localMap = map
        }
        val localMap2: MutableMap<String?, Any?>
        if (map.containsKey(BOOLEAN) && TRUE == map[BOOLEAN] && map.containsKey(TEXT)) {
            localMap2 = (localMap as LinkedHashMap<String?, Any?>).clone() as MutableMap<String?, Any?>
            localMap2.remove(BOOLEAN)
            localMap2[TEXT] = localMap[TEXT].toString().toBoolean()
        } else {
            localMap2 = localMap
        }
        return checkArray(localMap2, name)
    }

    private fun checkArray(map: MutableMap<String?, Any?>, name: String): Any {
        val localMap = checkNullAndString(map)
        val `object`: Any = if (map.containsKey(ARRAY) && TRUE == map[ARRAY]) {
            val localMap4: MutableMap<String?, Any?> =
                (localMap as LinkedHashMap<String?, Any?>).clone() as MutableMap<String?, Any?>
            localMap4.remove(ARRAY)
            localMap4.remove(SELF_CLOSING)
            if (name == XmlValue.getMapKey(localMap4)) ArrayList(
                listOf(
                    getValue(
                        name,
                        XmlValue.getMapValue(localMap4),
                        FromType.FOR_CONVERT
                    )
                )
            ) else ArrayList(
                listOf(
                    getValue(name, localMap4, FromType.FOR_CONVERT)
                )
            )
        } else {
            localMap
        }
        val object2: Any
        if (map.containsKey(EMPTY_ARRAY) && TRUE == map[EMPTY_ARRAY]) {
            val localMap4: MutableMap<String?, Any?> =
                (map as LinkedHashMap<String?, Any?>).clone() as MutableMap<String?, Any?>
            localMap4.remove(EMPTY_ARRAY)
            if (localMap4.containsKey(ARRAY) && TRUE == localMap4[ARRAY] && localMap4.size == 1) {
                object2 = ArrayList<Any>()
                (object2 as MutableList<Any?>).add(ArrayList<Any?>())
            } else {
                object2 = if (localMap4.isEmpty()) ArrayList<Any>() else localMap4
            }
        } else {
            object2 = `object`
        }
        return object2
    }

    private fun checkNullAndString(map: MutableMap<String?, Any?>): Map<String?, Any?> {
        val localMap: MutableMap<String?, Any?>
        if (map.containsKey(NULL_ATTR) && TRUE == map[NULL_ATTR]) {
            localMap = (map as LinkedHashMap<String?, Any?>).clone() as MutableMap<String?, Any?>
            localMap.remove(NULL_ATTR)
            if (!map.containsKey(TEXT)) {
                localMap[TEXT] = null
            }
        } else {
            localMap = map
        }
        val localMap2: MutableMap<String?, Any?>
        if (map.containsKey(STRING) && TRUE == map[STRING]) {
            localMap2 = (localMap as LinkedHashMap<String?, Any?>).clone() as MutableMap<String?, Any?>
            localMap2.remove(STRING)
            if (!map.containsKey(TEXT)) {
                localMap2[TEXT] = ""
            }
        } else {
            localMap2 = localMap
        }
        return localMap2
    }

    private fun addElement(
        sourceIndex: IntArray,
        source: String,
        elementMapper: BiFunction<Any, Set<String>, String?>,
        nodeMapper: Function<Any?, Any?>,
        uniqueIds: IntArray,
        currentNode: Node,
        namespaces: MutableSet<String>,
        fromType: FromType
    ): Any {
        val attrMapLocal = LinkedHashMap<String?, Any?>()
        if (currentNode.attributes.length > 0) {
            val attributes = parseAttributes(getAttributes(sourceIndex[0], source))
            for ((key) in attributes) {
                if (key.startsWith("xmlns:")) {
                    namespaces.add(key.substring(6))
                }
            }
            for ((key, value) in attributes) {
                addNodeValue(
                    attrMapLocal,
                    "-$key",
                    value,
                    elementMapper,
                    nodeMapper,
                    uniqueIds,
                    namespaces,
                    fromType
                )
            }
        }
        if (getAttributes(sourceIndex[0], source).endsWith("/")
            && !attrMapLocal.containsKey(SELF_CLOSING)
            && (attrMapLocal.size != 1
                    || ((!attrMapLocal.containsKey(STRING)
                    || TRUE != attrMapLocal[STRING])
                    && (!attrMapLocal.containsKey(NULL_ATTR)
                    || TRUE != attrMapLocal[NULL_ATTR])))
        ) {
            attrMapLocal[SELF_CLOSING] = TRUE
        }
        return createMap(
            currentNode,
            elementMapper,
            nodeMapper,
            attrMapLocal,
            uniqueIds,
            source,
            sourceIndex,
            namespaces,
            fromType
        )
    }

    @JvmStatic
    fun parseAttributes(source: String): Map<String, String> {
        val result: MutableMap<String, String> = LinkedHashMap()
        val key = StringBuilder()
        val value = StringBuilder()
        var quoteFound = false
        var equalFound = false
        var index = 0
        while (index < source.length) {
            if (source[index] == '=') {
                equalFound = !equalFound
                index += 1
                continue
            }
            if (source[index] == '"') {
                if (quoteFound && equalFound) {
                    result[key.toString()] = value.toString()
                    key.setLength(0)
                    value.setLength(0)
                    equalFound = false
                }
                quoteFound = !quoteFound
            } else if (quoteFound || source[index] == ' ') {
                if (quoteFound) {
                    value.append(source[index])
                }
            } else {
                key.append(source[index])
            }
            index += 1
        }
        return result
    }

    @JvmStatic
    fun getAttributes(sourceIndex: Int, source: String): String {
        var scanQuote = false
        var index = sourceIndex
        while (index < source.length) {
            if (source[index] == '"') {
                scanQuote = !scanQuote
                index += 1
                continue
            }
            if (!scanQuote && source[index] == '>') {
                return source.substring(sourceIndex, index)
            }
            index += 1
        }
        return ""
    }

    private fun unescapeName(name: String?): String? {
        if (name == null) {
            return null
        }
        val length = name.length
        if ("__EE__EMPTY__EE__" == name) {
            return ""
        }
        if ("-__EE__EMPTY__EE__" == name) {
            return "-"
        }
        if (!name.contains("__")) {
            return name
        }
        val result = StringBuilder()
        var underlineCount = 0
        val lastChars = StringBuilder()
        var i = 0
        outer@ while (i < length) {
            val ch = name[i]
            if (ch == '_') {
                lastChars.append(ch)
            } else {
                if (lastChars.length == 2) {
                    val nameToDecode = StringBuilder()
                    for (j in i until length) {
                        if (name[j] == '_') {
                            underlineCount += 1
                            if (underlineCount == 2) {
                                try {
                                    result.append(decode(nameToDecode.toString()))
                                } catch (ex: DecodingException) {
                                    result.append("__").append(nameToDecode).append(lastChars)
                                }
                                i = j
                                underlineCount = 0
                                lastChars.setLength(0)
                                i++
                                continue@outer
                            }
                        } else {
                            nameToDecode.append(name[j])
                            underlineCount = 0
                        }
                    }
                }
                result.append(lastChars).append(ch)
                lastChars.setLength(0)
            }
            i++
        }
        return result.append(lastChars).toString()
    }

    private fun addNodeValue(
        map: MutableMap<String?, Any?>,
        name: String,
        value: Any?,
        elementMapper: BiFunction<Any, Set<String>, String?>,
        nodeMapper: Function<Any?, Any?>,
        uniqueIds: IntArray,
        namespaces: Set<String>,
        fromType: FromType
    ) {
        val elementName = unescapeName(elementMapper.apply(name, namespaces))
        if (map.containsKey(elementName)) {
            if (TEXT == elementName) {
                map[elementName + uniqueIds[0]] = nodeMapper.apply(getValue(name, value, fromType))
                uniqueIds[0] += 1
            } else if (COMMENT == elementName) {
                map[elementName + uniqueIds[1]] = nodeMapper.apply(getValue(name, value, fromType))
                uniqueIds[1] += 1
            } else if (CDATA == elementName) {
                map[elementName + uniqueIds[2]] = nodeMapper.apply(getValue(name, value, fromType))
                uniqueIds[2] += 1
            } else {
                val `object` = map[elementName]
                if (`object` is List<*>) {
                    addText(map, elementName, `object` as MutableList<Any?>, value, fromType)
                } else {
                    val objects = ArrayList<Any?>()
                    objects.add(`object`)
                    addText(map, elementName, objects, value, fromType)
                    map[elementName] = objects
                }
            }
        } else {
            if (elementName != null) {
                map[elementName] = nodeMapper.apply(getValue(name, value, fromType))
            }
        }
    }

    private fun addText(
        map: MutableMap<String?, Any?>,
        name: String?,
        objects: MutableList<Any?>,
        value: Any?,
        fromType: FromType
    ) {
        var lastIndex = map.size - 1
        val index = objects.size
        while (true) {
            val (key) = map.entries.toTypedArray()[lastIndex]
            if (name == key.toString()) {
                break
            }
            val item = LinkedHashMap<String, Any>()
            val text = LinkedHashMap<String, Any?>()
            text[key.toString()] = map.remove(key)
            item["#item"] = text
            objects.add(index, item)
            lastIndex -= 1
        }
        val newValue = getValue(name, value, fromType)
        if (newValue is List<*>) {
            objects.add(newValue[0])
        } else {
            objects.add(newValue)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun fromXml(xml: String?, fromType: FromType = FromType.FOR_CONVERT): Any? {
        return if (xml == null) {
            null
        } else try {
            val document = Document.createDocument(xml)
            val result = createMap(
                document,
                { `object`: Any, _: Set<String>? -> `object`.toString() },
                { `object`: Any? -> `object` }, emptyMap<String?, Any>(), intArrayOf(1, 1, 1),
                xml, intArrayOf(0),
                LinkedHashSet(),
                fromType
            )
            if (checkResult(xml, document, result, fromType)) {
                ((result as Map<*, *>).entries.iterator().next() as Map.Entry<*, *>).value
            } else result
        } catch (ex: Exception) {
            throw IllegalArgumentException(ex)
        }
    }

    private fun checkResult(
        xml: String,
        document: org.w3c.dom.Document,
        result: Any,
        fromType: FromType
    ): Boolean {
        val headerAttributes = getHeaderAttributes(xml)
        if (document.xmlEncoding != null
            && !"UTF-8".equals(document.xmlEncoding, ignoreCase = true)
        ) {
            (result as MutableMap<String?, Any?>)[ENCODING] = document.xmlEncoding
            if (headerAttributes.containsKey(STANDALONE.substring(1))) {
                result[STANDALONE] = headerAttributes[STANDALONE.substring(1)]
            }
        } else if (headerAttributes.containsKey(STANDALONE.substring(1))) {
            (result as MutableMap<String?, Any?>)[STANDALONE] = headerAttributes[STANDALONE.substring(1)]
        } else if (fromType == FromType.FOR_CONVERT && XmlValue.getMapKey(result) == ROOT && (XmlValue.getMapValue(
                result
            ) is List<*>
                    || XmlValue.getMapValue(result) is Map<*, *>)
        ) {
            if (xml.startsWith(XML_HEADER)) {
                return true
            } else {
                (result as MutableMap<String?, Any?>)[OMITXMLDECLARATION] = YES
            }
        } else if (!xml.startsWith(XML_HEADER)) {
            (result as MutableMap<String?, Any?>)[OMITXMLDECLARATION] = YES
        }
        return false
    }

    private fun getHeaderAttributes(xml: String): Map<String, String> {
        val result: MutableMap<String, String> = LinkedHashMap()
        if (xml.startsWith(XML_HEADER)) {
            val xmlLocal = xml.substring(
                XML_HEADER.length,
                max(XML_HEADER.length.toDouble(), xml.indexOf("?>", XML_HEADER.length).toDouble()).toInt()
            )
            val attributes = parseAttributes(xmlLocal)
            for ((key, value) in attributes) {
                result[key] = value
            }
        }
        return result
    }

    @JvmStatic
    fun getDoctypeValue(xml: String): String {
        val startIndex = xml.indexOf(DOCTYPE_HEADER) + DOCTYPE_HEADER.length
        var charToFind = '>'
        var endIndexPlus = 0
        var endIndex = startIndex
        while (endIndex < xml.length) {
            if (xml[endIndex] == '[') {
                charToFind = ']'
                endIndexPlus = 1
                endIndex += 1
                continue
            }
            if (xml[endIndex] == charToFind) {
                return xml.substring(startIndex, endIndex + endIndexPlus)
            }
            endIndex += 1
        }
        return ""
    }

    @JvmStatic
    fun fromXmlMakeArrays(xml: String): Any {
        return try {
            val document = Document.createDocument(xml)
            val result = createMap(
                document,
                { `object`: Any, _: Set<String>? -> `object`.toString() },
                { `object`: Any? -> `object` as? List<*> ?: ArrayList(listOf(`object`)) },
                emptyMap<String?, Any>(),
                intArrayOf(1, 1, 1),
                xml,
                intArrayOf(0),
                LinkedHashSet(),
                FromType.FOR_CONVERT
            )
            if (checkResult(xml, document, result, FromType.FOR_CONVERT)) {
                ((result as Map<*, *>).entries.iterator().next() as Map.Entry<*, *>).value!!
            } else result
        } catch (ex: Exception) {
            throw IllegalArgumentException(ex)
        }
    }

    private fun fromXmlWithElementMapper(
        xml: String, elementMapper: BiFunction<Any, Set<String>, String?>
    ): Any {
        return try {
            val document = Document.createDocument(xml)
            val result = createMap(
                document,
                elementMapper,
                { `object`: Any? -> `object` }, emptyMap<String?, Any>(), intArrayOf(1, 1, 1),
                xml, intArrayOf(0),
                LinkedHashSet(),
                FromType.FOR_CONVERT
            )
            if (checkResult(xml, document, result, FromType.FOR_CONVERT)) {
                ((result as Map<*, *>).entries.iterator().next() as Map.Entry<*, *>).value!!
            } else result
        } catch (ex: Exception) {
            throw IllegalArgumentException(ex)
        }
    }

    @JvmStatic
    fun fromXmlWithoutNamespaces(xml: String): Any {
        return fromXmlWithElementMapper(
            xml
        ) { `object`: Any, namespaces: Set<String> ->
            val localString = `object`.toString()
            val result: String = if (localString.startsWith("-")
                && namespaces.contains(
                    localString.substring(
                        1, Math.max(1, localString.indexOf(':'))
                    )
                )
            ) {
                ("-"
                        + localString.substring(
                    Math.max(0, localString.indexOf(':') + 1)
                ))
            } else if (namespaces.contains(
                    localString.substring(0, Math.max(0, localString.indexOf(':')))
                )
            ) {
                localString.substring(Math.max(0, localString.indexOf(':') + 1))
            } else {
                `object`.toString()
            }
            result
        }
    }

    @JvmStatic
    fun fromXmlWithoutAttributes(xml: String): Any {
        return fromXmlWithElementMapper(
            xml
        ) { `object`: Any, _: Set<String>? ->
            if (`object`.toString().startsWith("-")) null else `object`.toString()
        }
    }

    @JvmStatic
    fun fromXmlWithoutNamespacesAndAttributes(xml: String): Any {
        return fromXmlWithElementMapper(
            xml
        ) { `object`: Any, namespaces: Set<String> ->
            val localString = `object`.toString()
            val result: String? = if (localString.startsWith("-")) {
                null
            } else if (namespaces.contains(
                    localString.substring(0, Math.max(0, localString.indexOf(':')))
                )
            ) {
                localString.substring(Math.max(0, localString.indexOf(':') + 1))
            } else {
                `object`.toString()
            }
            result
        }
    }

    @JvmStatic
    @JvmOverloads
    fun formatXml(xml: String?, identStep: XmlStringBuilder.Step = XmlStringBuilder.Step.TWO_SPACES): String {
        val result = fromXml(xml, FromType.FOR_FORMAT)
        return toXml(result as Map<*, *>?, identStep, ROOT)
    }

    @JvmStatic
    fun changeXmlEncoding(
        xml: String?, identStep: XmlStringBuilder.Step, encoding: String?
    ): String? {
        val result = fromXml(xml, FromType.FOR_FORMAT)
        if (result is Map<*, *>) {
            (result as MutableMap<String?, Any?>)[ENCODING] = encoding
            return toXml(result as Map<*, *>?, identStep, ROOT)
        }
        return xml
    }

    @JvmStatic
    fun changeXmlEncoding(xml: String?, encoding: String?): String? {
        return changeXmlEncoding(xml, XmlStringBuilder.Step.TWO_SPACES, encoding)
    }

    enum class ArrayTrue {
        ADD,
        SKIP
    }

    open class XmlStringBuilder {
        enum class Step(val ident: Int) {
            TWO_SPACES(2),
            THREE_SPACES(3),
            FOUR_SPACES(4),
            COMPACT(0),
            TABS(1)

        }

        protected val builder: StringBuilder
        val identStep: Step
        var ident: Int
            private set

        constructor() {
            builder = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root>\n")
            identStep = Step.TWO_SPACES
            ident = 2
        }

        constructor(builder: StringBuilder, identStep: Step, ident: Int) {
            this.builder = builder
            this.identStep = identStep
            this.ident = ident
        }

        fun append(string: String?): XmlStringBuilder {
            builder.append(string)
            return this
        }

        fun fillSpaces(): XmlStringBuilder {
            builder.append(
                (if (identStep == Step.TABS) '\t' else ' ').toString().repeat(Math.max(0, ident))
            )
            return this
        }

        fun incIdent(): XmlStringBuilder {
            ident += identStep.ident
            return this
        }

        fun decIdent(): XmlStringBuilder {
            ident -= identStep.ident
            return this
        }

        fun newLine(): XmlStringBuilder {
            if (identStep != Step.COMPACT) {
                builder.append("\n")
            }
            return this
        }

        override fun toString(): String {
            return "$builder\n</root>"
        }
    }

    class XmlStringBuilderWithoutRoot(
        identStep: Step, encoding: String?, standalone: String
    ) : XmlStringBuilder(
        StringBuilder(
            "<?xml version=\"1.0\" encoding=\""
                    + XmlValue.escape(encoding).replace("\"", QUOT)
                    + "\""
                    + standalone
                    + "?>"
                    + if (identStep == Step.COMPACT) "" else "\n"
        ),
        identStep,
        0
    ) {
        override fun toString(): String {
            return builder.toString()
        }
    }

    open class XmlStringBuilderWithoutHeader(identStep: Step, ident: Int) :
        XmlStringBuilder(StringBuilder(), identStep, ident) {
        override fun toString(): String {
            return builder.toString()
        }
    }

    class XmlStringBuilderText(identStep: Step, ident: Int) : XmlStringBuilderWithoutHeader(identStep, ident)
    object XmlArray {
        @JvmStatic
        fun writeXml(
            collection: Collection<*>?,
            name: String?,
            builder: XmlStringBuilder,
            parentTextFound: Boolean,
            namespaces: MutableSet<String?>,
            addArray: Boolean,
            arrayTrue: String
        ) {
            if (collection == null) {
                builder.append(NULL)
                return
            }
            if (name != null) {
                builder.fillSpaces().append("<").append(XmlValue.escapeName(name, namespaces))
                if (addArray) {
                    builder.append(arrayTrue)
                }
                if (collection.isEmpty()) {
                    builder.append(" empty-array=\"true\"")
                }
                builder.append(">").incIdent()
                if (!collection.isEmpty()) {
                    builder.newLine()
                }
            }
            writeXml(collection, builder, name, parentTextFound, namespaces, arrayTrue)
            if (name != null) {
                builder.decIdent()
                if (!collection.isEmpty()) {
                    builder.newLine().fillSpaces()
                }
                builder.append("</").append(XmlValue.escapeName(name, namespaces)).append(">")
            }
        }

        fun writeXml(
            collection: Collection<*>,
            builder: XmlStringBuilder,
            name: String?,
            parentTextFound: Boolean,
            namespaces: MutableSet<String?>,
            arrayTrue: String
        ) {
            var localParentTextFound = parentTextFound
            val entries = ArrayList(collection)
            var index = 0
            while (index < entries.size) {
                val value = entries[index]
                val addNewLine = (index < entries.size - 1
                        && !XmlValue.getMapKey(XmlValue.getMapValue(entries[index + 1]))
                    .startsWith(TEXT))
                if (value == null) {
                    builder.fillSpaces()
                        .append(
                            "<"
                                    + (if (name == null) ELEMENT_TEXT else XmlValue.escapeName(name, namespaces))
                                    + (if (collection.size == 1) arrayTrue else "")
                                    + NULL_TRUE
                        )
                } else {
                    if (value is Map<*, *> && value.size == 1 && XmlValue.getMapKey(value) == "#item" && XmlValue.getMapValue(
                            value
                        ) is Map<*, *>
                    ) {
                        XmlObject.writeXml(
                            XmlValue.getMapValue(value) as Map<*, *>?,
                            null,
                            builder,
                            localParentTextFound,
                            namespaces,
                            true,
                            arrayTrue
                        )
                        if (XmlValue.getMapKey(XmlValue.getMapValue(value)).startsWith(TEXT)) {
                            localParentTextFound = true
                            index += 1
                            continue
                        }
                    } else {
                        XmlValue.writeXml(
                            value,
                            name ?: ELEMENT_TEXT,
                            builder,
                            localParentTextFound,
                            namespaces,
                            collection.size == 1 || value is Collection<*>,
                            arrayTrue
                        )
                    }
                    localParentTextFound = false
                }
                if (addNewLine) {
                    builder.newLine()
                }
                index += 1
            }
        }

        @JvmStatic
        fun writeXml(array: ByteArray?, builder: XmlStringBuilder) {
            if (array == null) {
                builder.fillSpaces().append(NULL_ELEMENT)
            } else if (array.isEmpty()) {
                builder.fillSpaces().append(EMPTY_ELEMENT)
            } else {
                for (i in array.indices) {
                    builder.fillSpaces().append(ELEMENT)
                    builder.append(array[i].toString())
                    builder.append(CLOSED_ELEMENT)
                    if (i != array.size - 1) {
                        builder.newLine()
                    }
                }
            }
        }

        @JvmStatic
        fun writeXml(array: ShortArray?, builder: XmlStringBuilder) {
            if (array == null) {
                builder.fillSpaces().append(NULL_ELEMENT)
            } else if (array.isEmpty()) {
                builder.fillSpaces().append(EMPTY_ELEMENT)
            } else {
                for (i in array.indices) {
                    builder.fillSpaces().append(ELEMENT)
                    builder.append(array[i].toString())
                    builder.append(CLOSED_ELEMENT)
                    if (i != array.size - 1) {
                        builder.newLine()
                    }
                }
            }
        }

        @JvmStatic
        fun writeXml(array: IntArray?, builder: XmlStringBuilder) {
            if (array == null) {
                builder.fillSpaces().append(NULL_ELEMENT)
            } else if (array.isEmpty()) {
                builder.fillSpaces().append(EMPTY_ELEMENT)
            } else {
                for (i in array.indices) {
                    builder.fillSpaces().append(ELEMENT)
                    builder.append(array[i].toString())
                    builder.append(CLOSED_ELEMENT)
                    if (i != array.size - 1) {
                        builder.newLine()
                    }
                }
            }
        }

        @JvmStatic
        fun writeXml(array: LongArray?, builder: XmlStringBuilder) {
            if (array == null) {
                builder.fillSpaces().append(NULL_ELEMENT)
            } else if (array.isEmpty()) {
                builder.fillSpaces().append(EMPTY_ELEMENT)
            } else {
                for (i in array.indices) {
                    builder.fillSpaces().append(ELEMENT)
                    builder.append(array[i].toString())
                    builder.append(CLOSED_ELEMENT)
                    if (i != array.size - 1) {
                        builder.newLine()
                    }
                }
            }
        }

        @JvmStatic
        fun writeXml(array: FloatArray?, builder: XmlStringBuilder) {
            if (array == null) {
                builder.fillSpaces().append(NULL_ELEMENT)
            } else if (array.isEmpty()) {
                builder.fillSpaces().append(EMPTY_ELEMENT)
            } else {
                for (i in array.indices) {
                    builder.fillSpaces().append(ELEMENT)
                    builder.append(array[i].toString())
                    builder.append(CLOSED_ELEMENT)
                    if (i != array.size - 1) {
                        builder.newLine()
                    }
                }
            }
        }

        @JvmStatic
        fun writeXml(array: DoubleArray?, builder: XmlStringBuilder) {
            if (array == null) {
                builder.fillSpaces().append(NULL_ELEMENT)
            } else if (array.isEmpty()) {
                builder.fillSpaces().append(EMPTY_ELEMENT)
            } else {
                for (i in array.indices) {
                    builder.fillSpaces().append(ELEMENT)
                    builder.append(array[i].toString())
                    builder.append(CLOSED_ELEMENT)
                    if (i != array.size - 1) {
                        builder.newLine()
                    }
                }
            }
        }

        @JvmStatic
        fun writeXml(array: BooleanArray?, builder: XmlStringBuilder) {
            if (array == null) {
                builder.fillSpaces().append(NULL_ELEMENT)
            } else if (array.isEmpty()) {
                builder.fillSpaces().append(EMPTY_ELEMENT)
            } else {
                for (i in array.indices) {
                    builder.fillSpaces().append(ELEMENT)
                    builder.append(array[i].toString())
                    builder.append(CLOSED_ELEMENT)
                    if (i != array.size - 1) {
                        builder.newLine()
                    }
                }
            }
        }

        @JvmStatic
        fun writeXml(array: CharArray?, builder: XmlStringBuilder) {
            if (array == null) {
                builder.fillSpaces().append(NULL_ELEMENT)
            } else if (array.isEmpty()) {
                builder.fillSpaces().append(EMPTY_ELEMENT)
            } else {
                for (i in array.indices) {
                    builder.fillSpaces().append(ELEMENT)
                    builder.append(array[i].toString())
                    builder.append(CLOSED_ELEMENT)
                    if (i != array.size - 1) {
                        builder.newLine()
                    }
                }
            }
        }

        @JvmStatic
        fun writeXml(
            array: Array<Any?>?,
            name: String?,
            builder: XmlStringBuilder,
            parentTextFound: Boolean,
            namespaces: MutableSet<String?>,
            arrayTrue: String
        ) {
            if (array == null) {
                builder.fillSpaces().append(NULL_ELEMENT)
            } else if (array.isEmpty()) {
                builder.fillSpaces().append(EMPTY_ELEMENT)
            } else {
                for (i in array.indices) {
                    XmlValue.writeXml(
                        array[i],
                        name ?: ELEMENT_TEXT,
                        builder,
                        parentTextFound,
                        namespaces,
                        false,
                        arrayTrue
                    )
                    if (i != array.size - 1) {
                        builder.newLine()
                    }
                }
            }
        }
    }

    object XmlObject {
        fun writeXml(
            map: Map<*, *>?,
            name: String?,
            builder: XmlStringBuilder,
            parentTextFound: Boolean,
            namespaces: MutableSet<String?>,
            addArray: Boolean,
            arrayTrue: String
        ) {
            if (map == null) {
                XmlValue.writeXml(NULL, name, builder, false, namespaces, addArray, arrayTrue)
                return
            }
            val elems = ArrayList<XmlStringBuilder>()
            val attrs = ArrayList<String>()
            val identStep = builder.identStep
            val ident = builder.ident + if (name == null) 0 else builder.identStep.ident
            val entries: List<Map.Entry<String?, Any?>> = ArrayList(map.entries) as List<Map.Entry<String?, Any?>>
            val attrKeys = LinkedHashSet<String>()
            fillNamespacesAndAttrs(map, namespaces, attrKeys)
            var index = 0
            while (index < entries.size) {
                val entry = entries[index]
                val addNewLine = (index < entries.size - 1
                        && !entries[index + 1].key.toString().startsWith(TEXT))
                if (entry.key.toString().startsWith("-")
                    && entry.value is String
                ) {
                    attrs.add(
                        " "
                                + XmlValue.escapeName(
                            entry.key.toString().substring(1), namespaces
                        )
                                + "=\""
                                + XmlValue.escape(entry.value.toString())
                            .replace("\"", QUOT)
                                + "\""
                    )
                } else if (entry.key.toString().startsWith(TEXT)) {
                    addText(entry, elems, identStep, ident, attrKeys, attrs)
                } else {
                    val localParentTextFound = (elems.isNotEmpty()
                            && elems[elems.size - 1] is XmlStringBuilderText
                            || parentTextFound)
                    processElements(
                        entry,
                        identStep,
                        ident,
                        addNewLine,
                        elems,
                        namespaces,
                        localParentTextFound,
                        arrayTrue
                    )
                }
                index += 1
            }
            if (addArray && !attrKeys.contains(ARRAY)) {
                attrs.add(arrayTrue)
            }
            addToBuilder(name, parentTextFound, builder, namespaces, attrs, elems)
        }

        private fun fillNamespacesAndAttrs(
            map: Map<*, *>, namespaces: MutableSet<String?>, attrKeys: MutableSet<String>
        ) {
            for ((key, value) in map.entries) {
                if (key.toString().startsWith("-")
                    && value !is Map<*, *>
                    && value !is List<*>
                ) {
                    if (key.toString().startsWith("-xmlns:")) {
                        namespaces.add(key.toString().substring(7))
                    }
                    attrKeys.add(key.toString())
                }
            }
        }

        private fun addToBuilder(
            name: String?,
            parentTextFound: Boolean,
            builder: XmlStringBuilder,
            namespaces: Set<String?>,
            attrs: MutableList<String>,
            elems: List<XmlStringBuilder>
        ) {
            val selfClosing = attrs.remove(" self-closing=\"true\"")
            addOpenElement(name, parentTextFound, builder, namespaces, selfClosing, attrs, elems)
            if (!selfClosing) {
                for (localBuilder1 in elems) {
                    builder.append(localBuilder1.toString())
                }
            }
            if (name != null) {
                builder.decIdent()
                if (elems.isNotEmpty()
                    && elems[elems.size - 1] !is XmlStringBuilderText
                ) {
                    builder.newLine().fillSpaces()
                }
                if (!selfClosing) {
                    builder.append("</").append(XmlValue.escapeName(name, namespaces)).append(">")
                }
            }
        }

        private fun addOpenElement(
            name: String?,
            parentTextFound: Boolean,
            builder: XmlStringBuilder,
            namespaces: Set<String?>,
            selfClosing: Boolean,
            attrs: List<String>,
            elems: List<XmlStringBuilder>
        ) {
            if (name != null) {
                if (!parentTextFound) {
                    builder.fillSpaces()
                }
                builder.append("<")
                    .append(XmlValue.escapeName(name, namespaces))
                    .append(U.join(attrs, ""))
                if (selfClosing) {
                    builder.append("/")
                }
                builder.append(">").incIdent()
                if (elems.isNotEmpty() && elems[0] !is XmlStringBuilderText) {
                    builder.newLine()
                }
            }
        }

        private fun processElements(
            entry: Map.Entry<*, *>,
            identStep: XmlStringBuilder.Step,
            ident: Int,
            addNewLine: Boolean,
            elems: MutableList<XmlStringBuilder>,
            namespaces: MutableSet<String?>,
            parentTextFound: Boolean,
            arrayTrue: String
        ) {
            if (entry.key.toString().startsWith(COMMENT)) {
                addComment(entry, identStep, ident, parentTextFound, addNewLine, elems)
            } else if (entry.key.toString().startsWith(CDATA)) {
                addCdata(entry, identStep, ident, addNewLine, elems)
            } else if (entry.value is List<*> && (entry.value as List<*>).isNotEmpty()) {
                addElements(identStep, ident, entry, namespaces, elems, addNewLine, arrayTrue)
            } else {
                addElement(identStep, ident, entry, namespaces, elems, addNewLine, arrayTrue)
            }
        }

        private fun addText(
            entry: Map.Entry<*, *>,
            elems: MutableList<XmlStringBuilder>,
            identStep: XmlStringBuilder.Step,
            ident: Int,
            attrKeys: Set<String>,
            attrs: MutableList<String>
        ) {
            if (entry.value is List<*>) {
                for (value in entry.value as List<*>) {
                    elems.add(
                        XmlStringBuilderText(identStep, ident)
                            .append(XmlValue.escape(value.toString()))
                    )
                }
            } else {
                if (entry.value is Number && !attrKeys.contains(NUMBER)) {
                    attrs.add(NUMBER_TEXT)
                } else if (entry.value is Boolean && !attrKeys.contains(BOOLEAN)) {
                    attrs.add(" boolean=\"true\"")
                } else if (entry.value == null && !attrKeys.contains(NULL_ATTR)) {
                    attrs.add(" null=\"true\"")
                    return
                } else if ("" == entry.value && !attrKeys.contains(STRING)) {
                    attrs.add(" string=\"true\"")
                    return
                }
                elems.add(
                    XmlStringBuilderText(identStep, ident)
                        .append(XmlValue.escape(entry.value.toString()))
                )
            }
        }

        private fun addElements(
            identStep: XmlStringBuilder.Step,
            ident: Int,
            entry: Map.Entry<*, *>,
            namespaces: MutableSet<String?>,
            elems: MutableList<XmlStringBuilder>,
            addNewLine: Boolean,
            arrayTrue: String
        ) {
            val parentTextFound = elems.isNotEmpty() && elems[elems.size - 1] is XmlStringBuilderText
            val localBuilder: XmlStringBuilder = XmlStringBuilderWithoutHeader(identStep, ident)
            XmlArray.writeXml(
                entry.value as List<*>,
                localBuilder, entry.key.toString(),
                parentTextFound,
                namespaces,
                arrayTrue
            )
            if (addNewLine) {
                localBuilder.newLine()
            }
            elems.add(localBuilder)
        }

        private fun addElement(
            identStep: XmlStringBuilder.Step,
            ident: Int,
            entry: Map.Entry<*, *>,
            namespaces: MutableSet<String?>,
            elems: MutableList<XmlStringBuilder>,
            addNewLine: Boolean,
            arrayTrue: String
        ) {
            val parentTextFound = elems.isNotEmpty() && elems[elems.size - 1] is XmlStringBuilderText
            val localBuilder: XmlStringBuilder = XmlStringBuilderWithoutHeader(identStep, ident)
            XmlValue.writeXml(
                entry.value, entry.key.toString(),
                localBuilder,
                parentTextFound,
                namespaces,
                false,
                arrayTrue
            )
            if (addNewLine) {
                localBuilder.newLine()
            }
            elems.add(localBuilder)
        }

        private fun addComment(
            entry: Map.Entry<*, *>,
            identStep: XmlStringBuilder.Step,
            ident: Int,
            parentTextFound: Boolean,
            addNewLine: Boolean,
            elems: MutableList<XmlStringBuilder>
        ) {
            if (entry.value is List<*>) {
                val iterator = (entry.value as List<*>).iterator()
                while (iterator.hasNext()) {
                    elems.add(
                        addCommentValue(
                            identStep,
                            ident, iterator.next().toString(),
                            parentTextFound,
                            iterator.hasNext() || addNewLine
                        )
                    )
                }
            } else {
                elems.add(
                    addCommentValue(
                        identStep,
                        ident, entry.value.toString(),
                        parentTextFound,
                        addNewLine
                    )
                )
            }
        }

        private fun addCommentValue(
            identStep: XmlStringBuilder.Step,
            ident: Int,
            value: String,
            parentTextFound: Boolean,
            addNewLine: Boolean
        ): XmlStringBuilder {
            val localBuilder: XmlStringBuilder = XmlStringBuilderWithoutHeader(identStep, ident)
            if (!parentTextFound) {
                localBuilder.fillSpaces()
            }
            localBuilder.append("<!--").append(value).append("-->")
            if (addNewLine) {
                localBuilder.newLine()
            }
            return localBuilder
        }

        private fun addCdata(
            entry: Map.Entry<*, *>,
            identStep: XmlStringBuilder.Step,
            ident: Int,
            addNewLine: Boolean,
            elems: MutableList<XmlStringBuilder>
        ) {
            if (entry.value is List<*>) {
                val iterator = (entry.value as List<*>).iterator()
                while (iterator.hasNext()) {
                    elems.add(
                        addCdataValue(
                            identStep,
                            ident, iterator.next().toString(),
                            iterator.hasNext() || addNewLine
                        )
                    )
                }
            } else {
                elems.add(
                    addCdataValue(
                        identStep, ident, entry.value.toString(), addNewLine
                    )
                )
            }
        }

        private fun addCdataValue(
            identStep: XmlStringBuilder.Step, ident: Int, value: String, addNewLine: Boolean
        ): XmlStringBuilder {
            val localBuilder: XmlStringBuilder = XmlStringBuilderText(identStep, ident)
            localBuilder.append("<![CDATA[").append(value).append("]]>")
            if (addNewLine) {
                localBuilder.newLine()
            }
            return localBuilder
        }
    }

    object XmlValue {
        fun writeXml(
            value: Any?,
            name: String?,
            builder: XmlStringBuilder,
            parentTextFound: Boolean,
            namespaces: MutableSet<String?>,
            addArray: Boolean,
            arrayTrue: String
        ) {
            if (value is Map<*, *>) {
                XmlObject.writeXml(
                    value as Map<*, *>?,
                    name,
                    builder,
                    parentTextFound,
                    namespaces,
                    addArray,
                    arrayTrue
                )
                return
            }
            if (value is Collection<*>) {
                XmlArray.writeXml(
                    value as Collection<*>?,
                    name,
                    builder,
                    parentTextFound,
                    namespaces,
                    addArray,
                    arrayTrue
                )
                return
            }
            if (!parentTextFound) {
                builder.fillSpaces()
            }
            if (value == null) {
                builder.append("<" + escapeName(name, namespaces) + NULL_TRUE)
            } else if (value is String) {
                if (value.isEmpty()) {
                    builder.append(
                        "<"
                                + escapeName(name, namespaces)
                                + if (addArray) arrayTrue else ""
                    )
                    if (name!!.startsWith("?")) {
                        builder.append("?>")
                    } else {
                        builder.append(" string=\"true\"/>")
                    }
                } else {
                    builder.append(
                        "<"
                                + escapeName(name, namespaces)
                                + (if (addArray) arrayTrue else "")
                                + if (name!!.startsWith("?")) " " else ">"
                    )
                    builder.append(escape(value as String?))
                    if (name.startsWith("?")) {
                        builder.append("?>")
                    } else {
                        builder.append("</" + escapeName(name, namespaces) + ">")
                    }
                }
            } else {
                processArrays(
                    value, builder, name, parentTextFound, namespaces, addArray, arrayTrue
                )
            }
        }

        private fun processArrays(
            value: Any,
            builder: XmlStringBuilder,
            name: String?,
            parentTextFound: Boolean,
            namespaces: MutableSet<String?>,
            addArray: Boolean,
            arrayTrue: String
        ) {
            if (value is Double) {
                if (value.isInfinite() || value.isNaN()) {
                    builder.append(NULL_ELEMENT)
                } else {
                    builder.append(
                        "<"
                                + escapeName(name, namespaces)
                                + (if (addArray) arrayTrue else "")
                                + NUMBER_TRUE
                    )
                    builder.append(value.toString())
                    builder.append("</" + escapeName(name, namespaces) + ">")
                }
            } else if (value is Float) {
                if (value.isInfinite() || value.isNaN()) {
                    builder.append(NULL_ELEMENT)
                } else {
                    builder.append("<" + escapeName(name, namespaces) + NUMBER_TRUE)
                    builder.append(value.toString())
                    builder.append("</" + escapeName(name, namespaces) + ">")
                }
            } else if (value is Number) {
                builder.append(
                    "<"
                            + escapeName(name, namespaces)
                            + (if (addArray) arrayTrue else "")
                            + NUMBER_TRUE
                )
                builder.append(value.toString())
                builder.append("</" + escapeName(name, namespaces) + ">")
            } else if (value is Boolean) {
                builder.append(
                    "<"
                            + escapeName(name, namespaces)
                            + (if (addArray) arrayTrue else "")
                            + " boolean=\"true\">"
                )
                builder.append(value.toString())
                builder.append("</" + escapeName(name, namespaces) + ">")
            } else {
                builder.append("<" + escapeName(name, namespaces) + ">")
                if (value is ByteArray) {
                    builder.newLine().incIdent()
                    XmlArray.writeXml(value, builder)
                    builder.decIdent().newLine().fillSpaces()
                } else if (value is ShortArray) {
                    builder.newLine().incIdent()
                    XmlArray.writeXml(value, builder)
                    builder.decIdent().newLine().fillSpaces()
                } else {
                    processArrays2(value, builder, name, parentTextFound, namespaces, arrayTrue)
                }
                builder.append("</" + escapeName(name, namespaces) + ">")
            }
        }

        private fun processArrays2(
            value: Any,
            builder: XmlStringBuilder,
            name: String?,
            parentTextFound: Boolean,
            namespaces: MutableSet<String?>,
            arrayTrue: String
        ) {
            if (value is IntArray) {
                builder.newLine().incIdent()
                XmlArray.writeXml(value, builder)
                builder.decIdent().newLine().fillSpaces()
            } else if (value is LongArray) {
                builder.newLine().incIdent()
                XmlArray.writeXml(value, builder)
                builder.decIdent().newLine().fillSpaces()
            } else if (value is FloatArray) {
                builder.newLine().incIdent()
                XmlArray.writeXml(value, builder)
                builder.decIdent().newLine().fillSpaces()
            } else if (value is DoubleArray) {
                builder.newLine().incIdent()
                XmlArray.writeXml(value, builder)
                builder.decIdent().newLine().fillSpaces()
            } else if (value is BooleanArray) {
                builder.newLine().incIdent()
                XmlArray.writeXml(value, builder)
                builder.decIdent().newLine().fillSpaces()
            } else if (value is CharArray) {
                builder.newLine().incIdent()
                XmlArray.writeXml(value, builder)
                builder.decIdent().newLine().fillSpaces()
            } else if (value is Array<*> && value.isArrayOf<Any>()) {
                builder.newLine().incIdent()
                XmlArray.writeXml(
                    value as Array<Any?>, name, builder, parentTextFound, namespaces, arrayTrue
                )
                builder.decIdent().newLine().fillSpaces()
            } else {
                builder.append(value.toString())
            }
        }

        fun escapeName(name: String?, namespaces: Set<String?>): String {
            val length = name!!.length
            if (length == 0) {
                return "__EE__EMPTY__EE__"
            }
            val result = StringBuilder()
            var ch = name[0]
            if (ch != ':') {
                try {
                    if (ch != '?') {
                        DOCUMENT!!.createElement(ch.toString())
                    }
                    result.append(ch)
                } catch (ex: Exception) {
                    result.append("__").append(encode(Character.toString(ch))).append("__")
                }
            } else {
                result.append("__").append(encode(Character.toString(ch))).append("__")
            }
            for (i in 1 until length) {
                ch = name[i]
                if (ch == ':'
                    && ("xmlns" == name.substring(0, i) || namespaces.contains(name.substring(0, i)))
                ) {
                    result.append(ch)
                } else if (ch != ':') {
                    try {
                        DOCUMENT!!.createElement("a$ch")
                        result.append(ch)
                    } catch (ex: Exception) {
                        result.append("__")
                            .append(encode(Character.toString(ch)))
                            .append("__")
                    }
                } else {
                    result.append("__").append(encode(Character.toString(ch))).append("__")
                }
            }
            return result.toString()
        }

        @JvmStatic
        fun escape(s: String?): String {
            if (s == null) {
                return ""
            }
            val sb = StringBuilder()
            escape(s, sb)
            return sb.toString()
        }

        private fun escape(s: String, sb: StringBuilder) {
            val len = s.length
            for (i in 0 until len) {
                val ch = s[i]
                when (ch) {
                    '\'' -> sb.append("'")
                    '&' -> sb.append("&amp;")
                    '<' -> sb.append("&lt;")
                    '>' -> sb.append("&gt;")
                    '\b' -> sb.append("\\b")
                    '\u000c' -> sb.append("\\f")
                    '\n' -> sb.append("\n")
                    '\r' -> sb.append("&#xD;")
                    '\t' -> sb.append("\t")
                    '€' -> sb.append("€")
                    else -> if (ch <= '\u001F' || ch >= '\u007F' && ch <= '\u009F' || ch >= '\u2000' && ch <= '\u20FF') {
                        val ss = Integer.toHexString(ch.code)
                        sb.append("&#x")
                        sb.append("0".repeat(4 - ss.length))
                        sb.append(ss.uppercase(Locale.getDefault())).append(";")
                    } else {
                        sb.append(ch)
                    }
                }
            }
        }

        @JvmStatic
        fun unescape(s: String?): String {
            if (s == null) {
                return ""
            }
            val sb = StringBuilder()
            unescape(s, sb)
            return sb.toString()
        }

        private fun unescape(s: String, sb: StringBuilder) {
            val len = s.length
            val localSb = StringBuilder()
            var index = 0
            while (index < len) {
                val skipChars = translate(s, index, localSb)
                index += if (skipChars > 0) {
                    sb.append(localSb)
                    localSb.setLength(0)
                    skipChars
                } else {
                    sb.append(s[index])
                    1
                }
            }
        }

        private fun translate(
            input: CharSequence, index: Int, builder: StringBuilder
        ): Int {
            val shortest = 4
            val longest = 6
            if ('&' == input[index]) {
                var max = longest
                if (index + longest > input.length) {
                    max = input.length - index
                }
                for (i in max downTo shortest) {
                    val subSeq = input.subSequence(index, index + i)
                    val result = XML_UNESCAPE[subSeq.toString()]
                    if (result != null) {
                        builder.append(result)
                        return i
                    }
                }
            }
            return 0
        }

        @JvmStatic
        fun getMapKey(map: Any?): String {
            return if (map is Map<*, *> && map.isNotEmpty())
                (map.entries.iterator().next() as Map.Entry<*, *>).key.toString() else ""
        }

        @JvmStatic
        fun getMapValue(map: Any?): Any? {
            return if (map is Map<*, *> && map.isNotEmpty()) (map.entries.iterator()
                .next() as Map.Entry<*, *>).value else null
        }
    }

    enum class FromType {
        FOR_CONVERT,
        FOR_FORMAT
    }

    private class MyEntityResolver : EntityResolver {
        override fun resolveEntity(publicId: String?, systemId: String?): InputSource {
            return InputSource(StringReader(""))
        }
    }

    object Document {
        @JvmStatic
        @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
        fun createDocument(xml: String): org.w3c.dom.Document {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            try {
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            } catch (ignored: Exception) {
                // ignored
            }
            val builder = factory.newDocumentBuilder()
            builder.setErrorHandler(DefaultHandler())
            builder.setEntityResolver(MyEntityResolver())
            return builder.parse(InputSource(StringReader(xml)))
        }

        fun createDocument(): org.w3c.dom.Document? {
            return try {
                val factory = DocumentBuilderFactory.newInstance()
                factory.isNamespaceAware = true
                setupFactory(factory)
                val builder = factory.newDocumentBuilder()
                builder.newDocument()
            } catch (ex: ParserConfigurationException) {
                null
            }
        }

        private fun setupFactory(factory: DocumentBuilderFactory) {
            try {
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            } catch (ignored: Exception) {
                // ignored
            }
        }
    }
}
