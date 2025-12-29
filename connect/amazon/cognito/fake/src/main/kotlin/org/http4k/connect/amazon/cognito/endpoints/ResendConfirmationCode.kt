package org.http4k.connect.amazon.cognito.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.cognito.CognitoPool
import org.http4k.connect.amazon.cognito.action.ResendConfirmationCode
import org.http4k.connect.amazon.cognito.action.ResendConfirmationCodeResponse
import org.http4k.connect.amazon.cognito.model.AttributeName
import org.http4k.connect.amazon.cognito.model.CodeDeliveryDetails
import org.http4k.connect.amazon.cognito.model.DeliveryMedium
import org.http4k.connect.amazon.cognito.model.Destination
import org.http4k.connect.storage.Storage

fun AwsJsonFake.resendConfirmationCode(pools: Storage<CognitoPool>) = route<ResendConfirmationCode> {
    ResendConfirmationCodeResponse(
        CodeDeliveryDetails(
            AttributeName = AttributeName.of("email"),
            DeliveryMedium = DeliveryMedium.EMAIL,
            Destination = Destination.of("t***@example.com")
        )
    )
}
