package org.http4k.connect.amazon.dynamodb.grammar

import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import parser4k.Parser
import parser4k.commonparsers.Tokens
import parser4k.inOrder
import parser4k.map
import parser4k.oneOf
import parser4k.parseWith
import parser4k.skipFirst

object ExpressionAttributeName : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>): Parser<Expr> = inOrder(oneOf('#'), Tokens.identifier)
        .skipFirst().map { value ->
            Expr { item ->
                val name = item.names["#$value"] ?: error("missing name $value")
                item.item[name] ?: AttributeValue.Null()
            }
        }

    fun projection(parser: () -> Parser<Expr>): Parser<Expr> = inOrder(oneOf('#'), Tokens.identifier)
        .skipFirst().map { value ->
            Expr { item ->
                (item.names["#$value"] ?: error("missing name $value")).value.parseWith(parser()).eval(item)
            }
        }
}
