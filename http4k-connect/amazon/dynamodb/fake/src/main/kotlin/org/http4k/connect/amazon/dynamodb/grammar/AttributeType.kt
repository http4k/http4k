package org.http4k.connect.amazon.dynamodb.grammar

import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import org.http4k.connect.amazon.dynamodb.model.DynamoDataType
import parser4k.Parser
import parser4k.commonparsers.Tokens
import parser4k.commonparsers.token
import parser4k.inOrder
import parser4k.map
import parser4k.ref
import parser4k.skipWrapper

object AttributeType : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>): Parser<Expr> =
        inOrder(
            token("attribute_type"),
            token("("),
            ref(parser),
            token(","),
            Tokens.identifier,
            token(")")
        ).skipWrapper()
            .map { (_, attr, _, dynamoType) ->
                Expr { item ->
                    attr.eval(item).let {
                        AttributeValue::class.java.methods.find { it.name == "get" + DynamoDataType.valueOf(dynamoType).name }
                            ?.invoke(it) != null
                    }
                }
            }

}
