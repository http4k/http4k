package org.http4k.connect.amazon.dynamodb.grammar

import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import parser4k.Parser
import parser4k.commonparsers.Tokens.identifier
import parser4k.map

private val reservedWords by lazy {
    ConditionAttributeValue::class.java
        .getResourceAsStream("reservedWords.txt")!!
        .reader()
        .readLines()
}

object ConditionAttributeValue : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>): Parser<Expr> = identifier.map(::conditionAttributeValue)
}

private fun conditionAttributeValue(value: String) = Expr { item ->
    if (reservedWords.any { it.equals(value, ignoreCase = true) }) {
        throw DynamoDbConditionError("Attribute name is a reserved keyword; reserved keyword: $value")
    }
    item.item[AttributeName.of(value)] ?: AttributeValue.Null()
}
