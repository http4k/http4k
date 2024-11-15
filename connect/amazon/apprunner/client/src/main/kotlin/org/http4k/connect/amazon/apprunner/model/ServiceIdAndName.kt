import dev.forkhandles.values.NonBlankStringValueFactory
import org.http4k.connect.amazon.apprunner.model.ServiceId
import org.http4k.connect.amazon.apprunner.model.ServiceName
import org.http4k.connect.amazon.core.model.ResourceId

class ServiceIdAndName private constructor(value: String) : ResourceId(value) {
    val serviceId get() = ServiceId.of(value.substringAfterLast('/'))
    val serviceName get() = ServiceName.of(value.substringBefore('/'))

    companion object : NonBlankStringValueFactory<ServiceIdAndName>(::ServiceIdAndName) {
        fun of(name: ServiceName, id: ServiceId) = of("$name/$id")
    }
}
