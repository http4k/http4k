package org.http4k.connect.amazon.dynamodb.grammar

import parser4k.Parser
import parser4k.anyCharExcept
import parser4k.commonparsers.token
import parser4k.inOrder
import parser4k.map
import parser4k.ref

typealias ExprFactory = (() -> Parser<Expr>) -> Parser<Expr>

object In : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>): Parser<Expr> =
        inOrder(
            ref(parser),
            token("IN"),
            token("("),
            parser4k.repeat(anyCharExcept(')')).map { it.joinToString("") },
            token(")")
        ).map { (attr, _, _, values) ->
            Expr { item ->
                values.split(",")
                    .map { ExpressionAttributeValue(it.trim().trimStart(':')) }.map { it.eval(item) }
                    .contains(attr.eval(item))
            }
        }
}
