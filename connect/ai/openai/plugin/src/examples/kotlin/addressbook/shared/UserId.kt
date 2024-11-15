package addressbook.shared

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class UserId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<UserId>(::UserId)
}
